package com.sep.vox.domain.model.school;

public enum SchoolBalanceEntryType {
    /** The school tops up its balance -- always tied to a paid Order. */
    TOP_UP,
    /**
     * Charge for the part that EXCEEDS the quota bundled with the plan. Consumption within the quota is already recorded in
     * school_subscription_quota_records + ai_usage_records and is NOT repeated in this ledger.
     */
    OVERAGE_CHARGE,
    /**
     * The school moves money from its self-funded balance into the QUOTA wallet of a quota type (school_subscription_quota
     * _records.total_allocated_amount_vnd). The money leaves the balance but is NOT spending on AI -- it
     * moves to another pocket of the same school, so there is no cost_usd/fx_rate_used and no session is
     * attached.
     *
     * <p>ONE-WAY and CURRENTLY NOT reversible: there is no path back to the balance, no transfer to the other quota
     * type, and an ADJUSTMENT row is NOT the way to fix it -- it adds money back to the balance but cannot lower the
     * quota wallet (every write to school_subscription_quota_records only adds), so the school would hold both.
     * See FundQuotaFromBalanceUseCase.
     *
     * <p>An actor + quotaType are required -- see chk_school_balance_entries_quota_funding_traceable (V12).
     */
    QUOTA_FUNDING,
    /** Refund (e.g. the school paid an order twice) -- always tied to the original Order. */
    REFUND,
    /** Manual adjustment by a System Admin, an actor + a reason are required. */
    ADJUSTMENT
}
