package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.subscription.SchoolSubscriptionQuotaRecordJpaEntity;

public interface SpringDataSchoolSubscriptionQuotaRecordRepository extends JpaRepository<SchoolSubscriptionQuotaRecordJpaEntity, UUID> {
    List<SchoolSubscriptionQuotaRecordJpaEntity> findBySchoolSubscriptionId(UUID schoolSubscriptionId);
    Optional<SchoolSubscriptionQuotaRecordJpaEntity> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, String quotaType);

    // clearAutomatically=true for the exact same reason as addUsage below, and it applies to BOTH outcomes of this
    // statement: ConsumeQuotaService re-reads the row it just touched in the same transaction -- the success path
    // has buildResponse read it to return the balance AFTER deduction, the failure path has chargeOverage re-read it to compute the remaining
    // quota. Without the flag, both reads hit the CACHED entity from the first load in the method: the success path
    // returns the OLD used (so fundsExhausted misses exactly the spend that uses up the quota), and the failure path's
    // re-read becomes meaningless because it can never see anything new.
    //
    // flushAutomatically=true goes with it, following the general rule of this repo: whenever there is clearAutomatically
    // there must be a flush, because em.clear() discards every change NOT yet pushed to the DB, not only of this
    // table. See SpringDataSchoolSubscriptionQuotaUserAllocationRepository.addUsage for the case that nearly
    // lost money for lack of exactly this pair of flags.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolSubscriptionQuotaRecordJpaEntity q
        SET q.usedAmountVnd = q.usedAmountVnd + :amount
        WHERE q.id = :id AND q.usedAmountVnd + :amount <= q.totalAllocatedAmountVnd
        """)
    int tryConsume(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    // clearAutomatically=true is REQUIRED: the caller always re-reads this entity (before/after) in the SAME
    // transaction to compare the lock/debt state (SchoolLockedDueToDebt/SchoolDebtCapExceeded/
    // SchoolDebtCleared) -- without the flag, Hibernate returns the old CACHED entity from the earlier load instead of
    // re-reading the DB after the bulk update, so the before/after comparison is always wrong (even though the DB is right).
    //
    // flushAutomatically=true: same flag-pair rule as tryConsume above.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SchoolSubscriptionQuotaRecordJpaEntity q SET q.totalAllocatedAmountVnd = q.totalAllocatedAmountVnd + :amount WHERE q.id = :id")
    void addAllocation(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SchoolSubscriptionQuotaRecordJpaEntity q SET q.usedAmountVnd = q.usedAmountVnd + :amount WHERE q.id = :id")
    void addUsage(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    /**
     * Top-up from the self-funded balance into the quota wallet: adds to total AND records the part that came from the wallet, in ONE
     * statement.
     *
     * <p>Do NOT split into addAllocation() followed by a separate add of funded: two statements are two chances for one to
     * run and not the other, and the consequences are asymmetric. Adding total but forgetting funded makes the money the school
     * paid vanish on renewal day (exactly the bug V12 was created to prevent); adding funded but forgetting total
     * directly violates chk_..._funded_within_total. With one statement there is no in-between state.
     *
     * <p>The clearAutomatically/flushAutomatically pair follows the general rule of this repo: the use case
     * re-reads the row it just added in the same transaction to return the balance AFTER top-up, and the wallet deduction entry
     * is waiting in the persistence context when this statement runs.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolSubscriptionQuotaRecordJpaEntity q
        SET q.totalAllocatedAmountVnd = q.totalAllocatedAmountVnd + :amount,
            q.fundedFromBalanceVnd = q.fundedFromBalanceVnd + :amount
        WHERE q.id = :id
        """)
    void addFundingFromBalance(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    /**
     * The carry-over appointments that have COME DUE: the wallet of a period that has already started running but still points back to the source period.
     *
     * <p>Filters by {@code startDate} rather than by the status of the source period: the new period can spend right
     * when its date arrives (findInForceBySchoolId checks the date range), while SubscriptionExpiryJob may take
     * up to an hour to move the old period to EXPIRED. Relying on status would make the money appear later than it
     * deserves to be spendable.
     *
     * <p>Does not require the source period to have ended: when the new period's start date arrives the old period expires by
     * definition (the new period continues from its endDate -- see nextPeriodStart).
     */
    @Query("""
        SELECT q FROM SchoolSubscriptionQuotaRecordJpaEntity q
        JOIN SchoolSubscriptionJpaEntity s ON s.id = q.schoolSubscriptionId
        WHERE q.carryFundingFromSubscriptionId IS NOT NULL
          AND s.startDate <= :now
        """)
    List<SchoolSubscriptionQuotaRecordJpaEntity> findDueFundingCarries(@Param("now") Instant now);

    /**
     * Deletes the appointment -- makes the carry-over happen EXACTLY ONCE.
     *
     * <p>clearAutomatically because the job reads the record, then adds the money, then calls this statement; leaving the stale entity in
     * the persistence context opens the way for a later read to see the appointment still there.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolSubscriptionQuotaRecordJpaEntity q
        SET q.carryFundingFromSubscriptionId = NULL
        WHERE q.id = :id
        """)
    void clearFundingCarry(@Param("id") UUID id);
}
