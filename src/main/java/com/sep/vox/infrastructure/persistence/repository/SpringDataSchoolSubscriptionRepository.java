package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.subscription.SchoolSubscriptionJpaEntity;

public interface SpringDataSchoolSubscriptionRepository extends JpaRepository<SchoolSubscriptionJpaEntity, UUID> {
    Optional<SchoolSubscriptionJpaEntity> findFirstBySchoolIdAndStatus(UUID schoolId, String status);

    /**
     * The subscription period IN FORCE at {@code at} -- filters by DATE and not only by status.
     *
     * <p>Filtering by date is mandatory since early renewal was allowed: then a school has TWO rows both
     * ACTIVE, one running and one already paid for but only starting when the other expires.
     * {@code findFirstBySchoolIdAndStatus} would return one of the two arbitrarily, and everything read from
     * it -- quota, assessment guard, spend deduction -- would attach to the wrong period.
     *
     * <p>It also closes a gap in SubscriptionExpiryJob: the job runs every hour, so right after the expiry time
     * there is still a stretch where the old row has status ACTIVE. With a date filter that stretch does not exist --
     * the same reason SubscriptionPeriodGuardService deliberately compares start_date/end_date directly.
     *
     * <p>ORDER BY start_date DESC: if a data error makes two rows both cover {@code at}, take the
     * newest, instead of letting the order depend on how Postgres returns rows.
     */
    @Query("""
        SELECT s FROM SchoolSubscriptionJpaEntity s
        WHERE s.schoolId = :schoolId
          AND s.status = 'ACTIVE'
          AND s.startDate <= :at
          AND s.endDate > :at
        ORDER BY s.startDate DESC
        """)
    List<SchoolSubscriptionJpaEntity> findInForceBySchoolId(
        @Param("schoolId") UUID schoolId, @Param("at") Instant at);

    /**
     * The period with the farthest endDate that has not yet ended -- the anchor for a new renewal to continue from.
     *
     * <p>Takes the largest endDate rather than the running period: a school renewing twice in a row must have the
     * second renewal follow the first, not overlap it.
     */
    @Query("""
        SELECT s FROM SchoolSubscriptionJpaEntity s
        WHERE s.schoolId = :schoolId
          AND s.status = 'ACTIVE'
          AND s.endDate > :at
        ORDER BY s.endDate DESC
        """)
    List<SchoolSubscriptionJpaEntity> findUnfinishedBySchoolId(
        @Param("schoolId") UUID schoolId, @Param("at") Instant at);

    /**
     * The most recent period of the school, with NO filter by status or date -- to know which plan the school is/was
     * using in order to propose a renewal.
     *
     * <p>It must accept EXPIRED periods too: a late renewal (the plan expired last week and the school only came back now)
     * is the most common path, and there is no ACTIVE period left to ask about.
     */
    @Query("""
        SELECT s FROM SchoolSubscriptionJpaEntity s
        WHERE s.schoolId = :schoolId
        ORDER BY s.endDate DESC
        """)
    List<SchoolSubscriptionJpaEntity> findMostRecentBySchoolId(@Param("schoolId") UUID schoolId);
    List<SchoolSubscriptionJpaEntity> findBySchoolId(UUID schoolId);
    boolean existsBySubscriptionPlanIdAndStatus(UUID subscriptionPlanId, String status);

    // A bulk update bypasses the persistence context so it does NOT increment the @Version column on its own -- it must be incremented by hand
    // (s.version = s.version + 1) so that any record loaded elsewhere (e.g.
    // CancelSubscriptionUseCase) and then save()d after this job runs is detected by Hibernate as a
    // version mismatch and throws OptimisticLockException, instead of silently overwriting EXPIRED back to ACTIVE.
    //
    // Expiry applies to SUSPENDED too, not only ACTIVE. Suspension cuts off usage for the REMAINING part
    // of a paid period; once that period ends there is nothing left to suspend, and keeping the SUSPENDED
    // status would mix up "this period was once suspended" (history) with "the school is under sanction"
    // (state). Before, only ACTIVE was filtered so a SUSPENDED row never left that state, while
    // CreateSubscriptionOrderUseCase looks at the WHOLE history -- a 3-day suspension in last year's
    // period would forbid the school from buying again FOREVER.
    //
    // Expiring is NOT lifting the suspension: the suspended_* columns stay as they are, the school_subscription_events
    // ledger stays as it is, and the school regains no right to use anything (the right comes from an ACTIVE period
    // still in force, and EXPIRED is not one). It only gives back the right to BUY a new period. To forbid buying permanently it must be
    // a SCHOOL-level flag, not a subscription row sitting forever in SUSPENDED.
    @Modifying
    @Query("""
        UPDATE SchoolSubscriptionJpaEntity s
        SET s.status = 'EXPIRED', s.version = s.version + 1
        WHERE s.status IN ('ACTIVE', 'SUSPENDED') AND s.endDate < :cutoff
    """)
    int expireOverdue(@Param("cutoff") Instant cutoff);

    @Query("""
        SELECT s FROM SchoolSubscriptionJpaEntity s
        WHERE (:planId IS NULL OR s.subscriptionPlanId = :planId)
          AND (:status IS NULL OR s.status = :status)
          AND (:keywordPattern IS NULL OR EXISTS (
                SELECT 1 FROM SchoolJpaEntity sc
                WHERE sc.id = s.schoolId AND LOWER(sc.name) LIKE :keywordPattern
              ))
    """)
    Page<SchoolSubscriptionJpaEntity> findForAdmin(
        @Param("planId") UUID planId,
        @Param("status") String status,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    // THE NATIVE QUERIES BELOW follow the TARGET table/column names of V2, exactly like the @Column of
    // SchoolSubscriptionQuotaRecordJpaEntity / SchoolSubscriptionQuotaUserAllocationJpaEntity. Native
    // queries are not checked by Hibernate at startup, so a wrong name here only blows up on the first request
    // that touches it -- and findActiveSubscriptionIdForUser runs on EVERY practice speaking turn.
    //
    // The validity range is compared with CURRENT_TIMESTAMP, NOT CURRENT_DATE. V2 (section 19) changed
    // start_date/end_date to timestamptz, and Postgres promotes CURRENT_DATE to the START of the day's midnight --
    // so "CURRENT_DATE BETWEEN start AND end" returns false for the rest of the activation day: a school
    // that just paid at 3pm could only practice speaking after midnight. The other end is the opposite: a period that expired
    // at 3pm would still be counted alive for the rest of that day.
    //
    // Half-open (start <= now < end) to match EXACTLY findInForceBySchoolId above. The two are two
    // parallel doors to the same question "which period is in force" -- the assessment path asks with JPQL, the practice
    // path asks with a native query -- so if they disagree the system contradicts itself.
    @Query(value = """
        SELECT subscription.id
        FROM school_users school_user
        JOIN school_subscriptions subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND subscription.start_date <= CURRENT_TIMESTAMP
         AND subscription.end_date > CURRENT_TIMESTAMP
        WHERE school_user.user_id = :userId
          AND (school_user.end_date IS NULL OR school_user.end_date >= CURRENT_TIMESTAMP)
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<UUID> findActiveSubscriptionIdForUser(@Param("userId") UUID userId);

    // It must also LEFT JOIN school_subscription_quota_user_allocations and take LEAST(...): ConsumeQuotaService
    // deducts BOTH the school quota AND the personal quota (when an allocation row exists for the user) for PRACTICE, so
    // the gate in BuildPracticePaperUseCase must check exactly those 2 measures -- before, it only checked the school
    // quota, so a student who ran out of personal quota while the school still had some would slip through and the paper would be built, only to die at the
    // first speaking turn. COALESCE to 0 when the user has NO allocation row yet: the school must proactively allocate
    // a separate quota (manually or "Distribute evenly automatically") before the student can practice -- not allocated means
    // fully blocked, no longer falling back to the school's shared pool as before. LEAST must not be taken directly with NULL,
    // Postgres returns NULL for the whole expression.
    //
    // The SCHOOL side adds the self-funded wallet balance, the PERSONAL side does not: since ConsumeQuotaService charges
    // the part over the quota to the self-funded wallet, "how much the school can still pay" = remaining quota + balance,
    // so checking the quota alone would block students of a school that still has money. The personal quota is an internal
    // SPENDING CEILING and not a wallet -- adding the school's money to it would wipe out the very ceiling
    // the school admin just set. See ClassTestTokenQuotaGuardService, the parallel gate on the assessment path.
    //
    // GREATEST(..., 0) clamps a negative balance to 0: negative is DEBT, already blocked by SchoolSubscriptionDebtGuardService at
    // a separate gate; adding the negative directly here would deduct a second time from the school's remaining quota.
    // LATERAL so that expression is written only ONCE even though LEAST needs it on both sides.
    @Query(value = """
        SELECT LEAST(
            school_funds.amount_vnd,
            COALESCE(user_allocation.allocated_amount_vnd - user_allocation.used_amount_vnd, 0)
        )
        FROM school_users school_user
        JOIN school_subscriptions subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND subscription.start_date <= CURRENT_TIMESTAMP
         AND subscription.end_date > CURRENT_TIMESTAMP
        JOIN school_subscription_quota_records quota
          ON quota.school_subscription_id = subscription.id
         AND quota.quota_type = 'PRACTICE'
        LEFT JOIN school_subscription_quota_user_allocations user_allocation
          ON user_allocation.school_subscription_id = subscription.id
         AND user_allocation.quota_type = 'PRACTICE'
         AND user_allocation.user_id = school_user.user_id
        LEFT JOIN school_balances balance
          ON balance.school_id = school_user.school_id
        CROSS JOIN LATERAL (
            SELECT quota.total_allocated_amount_vnd - quota.used_amount_vnd
                 + GREATEST(COALESCE(balance.balance_vnd, 0), 0) AS amount_vnd
        ) school_funds
        WHERE school_user.user_id = :userId
          AND (school_user.end_date IS NULL OR school_user.end_date >= CURRENT_TIMESTAMP)
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    List<BigDecimal> findPracticeSpendableFundsVnd(@Param("userId") UUID userId);

    @Query(value = """
        SELECT plan.max_time_per_attempt_min
        FROM school_users school_user
        JOIN school_subscriptions subscription
          ON subscription.school_id = school_user.school_id
         AND subscription.status = 'ACTIVE'
         AND subscription.start_date <= CURRENT_TIMESTAMP
         AND subscription.end_date > CURRENT_TIMESTAMP
        JOIN subscription_plans plan
          ON plan.id = subscription.subscription_plan_id
        WHERE school_user.user_id = :userId
          AND (school_user.end_date IS NULL OR school_user.end_date >= CURRENT_TIMESTAMP)
        ORDER BY subscription.end_date DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Integer> findMaxTimePerAttemptMinForUser(@Param("userId") UUID userId);
}
