package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.subscription.SchoolSubscriptionQuotaUserAllocationJpaEntity;

public interface SpringDataSchoolSubscriptionQuotaUserAllocationRepository extends JpaRepository<SchoolSubscriptionQuotaUserAllocationJpaEntity, UUID> {
    List<SchoolSubscriptionQuotaUserAllocationJpaEntity> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, String quotaType);
    Optional<SchoolSubscriptionQuotaUserAllocationJpaEntity> findBySchoolSubscriptionIdAndQuotaTypeAndUserId(UUID schoolSubscriptionId, String quotaType, UUID userId);

    @Modifying
    @Query("""
        UPDATE SchoolSubscriptionQuotaUserAllocationJpaEntity a
        SET a.usedAmountVnd = a.usedAmountVnd + :amount
        WHERE a.id = :id AND a.usedAmountVnd + :amount <= a.allocatedAmountVnd
        """)
    int tryConsume(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    /**
     * Total of the quota promised to each person that they have NOT yet spent.
     *
     * <p>Clamping negatives is done with WHERE rather than CASE: a row that spent over its ceiling (addUsage is an unconditional
     * UPDATE so that can happen) is excluded from the sum instead of contributing a negative number. A teacher
     * who exceeded the ceiling is NOT a credit handed back to the school -- that money was really spent,
     * and adding the negative part would make the amount promised to others look smaller.
     *
     * <p>Sums the UNSPENT part and not the whole allocated amount, because this is the number that will be subtracted from the school
     * wallet: what the teacher already spent was already deducted by ConsumeQuotaService from the wallet's used_amount_vnd
     * (consumeUserAllocation adds the SAME amount to both counters), so subtracting the whole allocated amount
     * would deduct the same spent money twice.
     */
    // COALESCE to 0: SUM over an empty set returns NULL -- a school that has distributed to nobody is the MOST COMMON case.
    @Query("""
        SELECT COALESCE(SUM(a.allocatedAmountVnd - a.usedAmountVnd), 0)
        FROM SchoolSubscriptionQuotaUserAllocationJpaEntity a
        WHERE a.schoolSubscriptionId = :schoolSubscriptionId
          AND a.quotaType = :quotaType
          AND a.allocatedAmountVnd > a.usedAmountVnd
        """)
    BigDecimal sumUnusedAllocation(
        @Param("schoolSubscriptionId") UUID schoolSubscriptionId,
        @Param("quotaType") String quotaType);

    /**
     * Total distributed to the people who are STILL eligible to receive -- the same filter the quota distribution screen uses to
     * build its list ({@code SchoolUserRepository.findBySchoolId} with status ACTIVE + roleId).
     *
     * <p>It must filter rather than sum everything: allocation rows are NOT deleted when a teacher leaves or a student
     * graduates, so summing everything counts the part held by people who no longer appear on any page of the table. The school sees
     * "distributed" larger than the sum of the visible rows, and by the time that invisible part hits the ceiling every further
     * distribution is rejected without any screen being able to explain why.
     *
     * <p>Dropping them from the total does NOT create money out of thin air: what they already SPENT stays intact in the
     * school wallet's {@code used_amount_vnd} (ConsumeQuotaService.consumeUserAllocation adds the same
     * amount to both counters). What stops occupying room under the ceiling is only the part that nobody can spend any more.
     *
     * <p>Their rows are also NOT deleted: if the account is reactivated the old ceiling comes back
     * intact, and keeping them lets the books still trace who was ever granted how much.
     */
    @Query("""
        SELECT COALESCE(SUM(a.allocatedAmountVnd), 0)
        FROM SchoolSubscriptionQuotaUserAllocationJpaEntity a
        JOIN SchoolUserJpaEntity su ON su.userId = a.userId
        JOIN UserJpaEntity u ON u.id = a.userId
        WHERE a.schoolSubscriptionId = :schoolSubscriptionId
          AND a.quotaType = :quotaType
          AND su.schoolId = :schoolId
          AND u.status = :status
          AND EXISTS (
              SELECT 1 FROM UserRoleJpaEntity ur
              WHERE ur.userId = a.userId AND ur.roleId = :roleId)
        """)
    BigDecimal sumAllocatedForEligibleUsers(
        @Param("schoolSubscriptionId") UUID schoolSubscriptionId,
        @Param("quotaType") String quotaType,
        @Param("schoolId") UUID schoolId,
        @Param("roleId") UUID roleId,
        @Param("status") String status);

    /**
     * Total distributed over EVERY row, including people who are no longer eligible.
     *
     * <p>Only used to show the difference from {@link #sumAllocatedForEligibleUsers} -- that number is the
     * one compared against the ceiling. It is here so the "invisible" part can be put into words in the UI instead of
     * being silently left out of the calculation.
     */
    @Query("""
        SELECT COALESCE(SUM(a.allocatedAmountVnd), 0)
        FROM SchoolSubscriptionQuotaUserAllocationJpaEntity a
        WHERE a.schoolSubscriptionId = :schoolSubscriptionId
          AND a.quotaType = :quotaType
        """)
    BigDecimal sumAllocated(
        @Param("schoolSubscriptionId") UUID schoolSubscriptionId,
        @Param("quotaType") String quotaType);

    // clearAutomatically=true -- same reason as SpringDataSchoolSubscriptionQuotaRecordRepository.addUsage:
    // prevents Hibernate from returning an old cached entity if anything later re-reads in the same transaction.
    //
    // flushAutomatically=true is REQUIRED with it, not just for tidiness: ConsumeQuotaService calls
    // consumeUserAllocation AFTER chargeOverage, and chargeOverage has just merged a SchoolBalance that was already deducted and
    // persisted the OVERAGE_CHARGE entry -- both are still waiting in the persistence context. Hibernate only
    // auto-flushes the query spaces this UPDATE touches (the allocation table), NOT
    // school_balances / school_balance_entries, so the em.clear() afterwards throws the debit away: the AI
    // cost vanishes from the school wallet without any error surfacing, and the invariant SUM(entries) = balance_vnd breaks.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SchoolSubscriptionQuotaUserAllocationJpaEntity a SET a.usedAmountVnd = a.usedAmountVnd + :amount WHERE a.id = :id")
    void addUsage(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}
