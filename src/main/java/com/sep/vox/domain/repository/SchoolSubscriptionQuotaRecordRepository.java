package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;

public interface SchoolSubscriptionQuotaRecordRepository {
    Optional<SchoolSubscriptionQuotaRecord> findById(UUID id);
    SchoolSubscriptionQuotaRecord save(SchoolSubscriptionQuotaRecord quota);
    List<SchoolSubscriptionQuotaRecord> findBySchoolSubscriptionId(UUID schoolSubscriptionId);
    Optional<SchoolSubscriptionQuotaRecord> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, QuotaType quotaType);
    void addAllocation(UUID quotaId, BigDecimal amount);

    /**
     * Moves money from the self-funded balance into the quota wallet: adds to both {@code totalAllocatedAmountVnd} and
     * {@code fundedFromBalanceVnd} in the same statement. The caller MUST have already debited the balance and written the
     * QUOTA_FUNDING entry in the same transaction -- see FundQuotaFromBalanceUseCase.
     */
    void addFundingFromBalance(UUID quotaId, BigDecimal amount);

    /**
     * Wallets of periods whose start date HAS arrived and that still hold a pending appointment carrying self-funded money across (V13). Empty in the
     * normal state -- only an EARLY renewal creates an appointment, and only until the next job run.
     */
    List<SchoolSubscriptionQuotaRecord> findDueFundingCarries(Instant now);

    /**
     * Deletes the appointment of a wallet. Call it AFTER the money has been added, in the same transaction -- this is what turns the
     * carry-over into exactly once.
     */
    void clearFundingCarry(UUID quotaId);

    /** returns false if usedAmountVnd + amount would exceed totalAllocatedAmountVnd. */
    boolean tryConsume(UUID quotaId, BigDecimal amount);

    /**
     * Unconditional -- always succeeds. Only used to spend the REMAINING quota to fill it up when an
     * expense straddles the ceiling (ConsumeQuotaService.chargeOverage); the caller clamps amount to the exact remaining part itself.
     * NOT used to record debt any more: debt is now a negative balance_vnd, and usedAmountVnd exceeding
     * totalAllocatedAmountVnd is corrupt data and no longer a valid state.
     */
    void addUsage(UUID quotaId, BigDecimal amount);
}
