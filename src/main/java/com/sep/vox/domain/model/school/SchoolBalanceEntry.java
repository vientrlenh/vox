package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * The append-only ledger of SchoolBalance. SchoolBalance is only the AGGREGATE for atomic deduction; the source
 * of truth is this table -- SUM(amountVnd) must always match the aggregate balance.
 *
 * <p>The source reference uses TYPED columns (orderId / assessmentSessionId / actorId) instead of the polymorphic pair
 * (source_type, source_id) like the old invoice: each column can be a real FK, and a CHECK constrains exactly
 * which column is set according to entryType.
 */
public class SchoolBalanceEntry {

    private UUID id;
    private UUID schoolId;
    /** The subscription that was ACTIVE when this arose -- ONLY for tracing, the balance does not belong to any plan. */
    private UUID subscriptionId;
    private SchoolBalanceEntryType entryType;
    /** Positive = top-up/refund/upward adjustment, negative = deduction. */
    private BigDecimal amountVnd;
    /** Balance after the entry -- rebuilding a statement does not need to accumulate from the beginning. */
    private BigDecimal balanceAfterVnd;
    /** Source order: required for TOP_UP/REFUND. */
    private UUID orderId;
    /** The EXAM session that caused the deduction -- OVERAGE_CHARGE must have EXACTLY ONE of the two session columns. */
    private UUID assessmentSessionId;
    /** The SPEAKING PRACTICE session that caused the deduction -- a separate column because each session column is a real FK. */
    private UUID practiceSessionId;
    /** A REPORTING dimension for ViewTokenUsageTimeseries, not a separate wallet. */
    private QuotaType quotaType;
    /** The provider's ORIGINAL cost (Azure), kept in USD to reconcile back against ai_usage_records. */
    private BigDecimal costUsd;
    /** The exchange rate used to convert costUsd to amountVnd -- stored because the rate changes daily. */
    private BigDecimal fxRateUsed;
    /** The person who acted: required for ADJUSTMENT. */
    private UUID actorId;
    private String reason;
    private Instant occurredAt;

    public SchoolBalanceEntry() {}



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public SchoolBalanceEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(SchoolBalanceEntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmountVnd() {
        return amountVnd;
    }

    public void setAmountVnd(BigDecimal amountVnd) {
        this.amountVnd = amountVnd;
    }

    public BigDecimal getBalanceAfterVnd() {
        return balanceAfterVnd;
    }

    public void setBalanceAfterVnd(BigDecimal balanceAfterVnd) {
        this.balanceAfterVnd = balanceAfterVnd;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getAssessmentSessionId() {
        return assessmentSessionId;
    }

    public void setAssessmentSessionId(UUID assessmentSessionId) {
        this.assessmentSessionId = assessmentSessionId;
    }

    public UUID getPracticeSessionId() {
        return practiceSessionId;
    }

    public void setPracticeSessionId(UUID practiceSessionId) {
        this.practiceSessionId = practiceSessionId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(BigDecimal costUsd) {
        this.costUsd = costUsd;
    }

    public BigDecimal getFxRateUsed() {
        return fxRateUsed;
    }

    public void setFxRateUsed(BigDecimal fxRateUsed) {
        this.fxRateUsed = fxRateUsed;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    /**
     * A TOP-UP entry from a fully paid order.
     *
     * <p>The amount added to the wallet is the order's {@code subtotalAmountVnd}, NOT the total: the service fee
     * (charged_fee_vnd) is our own fee, and the school must not be able to spend it back. See Order.forTopUp.
     *
     * <p>quotaType/costUsd/fxRateUsed are all null: this is money coming in, not yet tied to any usage -- those three
     * columns only mean something for OVERAGE_CHARGE.
     */
    public static SchoolBalanceEntry forTopUp(UUID schoolId, UUID subscriptionId, UUID orderId,
            BigDecimal creditedAmountVnd, BigDecimal balanceAfterVnd, Instant now) {
        return builder()
            .schoolId(schoolId)
            .subscriptionId(subscriptionId)
            .entryType(SchoolBalanceEntryType.TOP_UP)
            .amountVnd(creditedAmountVnd)
            .balanceAfterVnd(balanceAfterVnd)
            .orderId(orderId)
            .occurredAt(now)
            .build();
    }

    /**
     * A DEDUCTION entry for the AI cost that exceeds the plan's quota, for an EXAM session.
     *
     * <p>{@code overageVnd} is the part that EXCEEDS, not the whole charge: the part that stays within the quota has
     * already been recorded in school_subscription_quota_records.used_amount_vnd, and recording it here would count the
     * same money twice -- see ConsumeQuotaService.chargeOverage.
     */
    public static SchoolBalanceEntry forAssessmentOverageCharge(UUID schoolId, UUID subscriptionId,
            UUID assessmentSessionId, QuotaType quotaType, BigDecimal overageVnd, BigDecimal balanceAfterVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed, Instant now) {
        return overageCharge(schoolId, subscriptionId, assessmentSessionId, null, quotaType,
            overageVnd, balanceAfterVnd, costUsd, fxRateUsed, now);
    }

    /** Like {@link #forAssessmentOverageCharge} but the deduction comes from a SPEAKING PRACTICE session. */
    public static SchoolBalanceEntry forPracticeOverageCharge(UUID schoolId, UUID subscriptionId,
            UUID practiceSessionId, QuotaType quotaType, BigDecimal overageVnd, BigDecimal balanceAfterVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed, Instant now) {
        return overageCharge(schoolId, subscriptionId, null, practiceSessionId, quotaType,
            overageVnd, balanceAfterVnd, costUsd, fxRateUsed, now);
    }

    /**
     * A TRANSFER entry of money from the self-funded wallet to the quota wallet of one quota type.
     *
     * <p>Takes a POSITIVE number and flips the sign itself, for the same reason as {@link #overageCharge}: the constraint
     * chk_school_balance_entries_quota_funding_traceable requires {@code amount_vnd < 0}, and making the caller remember
     * {@code .negate()} leaves a place to forget it.
     *
     * <p>{@code costUsd}/{@code fxRateUsed} are null and both session columns are null -- the constraint above demands
     * exactly that: this is not a charge for AI but money moving between pockets, so there is no provider invoice to
     * reconcile against and no session that caused it.
     *
     * <p>{@code actorId} is REQUIRED, unlike the system-generated OVERAGE_CHARGE: this is a human decision with no way
     * back, so the ledger must be able to say who pressed the button. {@code reason} is optional -- this is a routine
     * operation, not a manual fix like ADJUSTMENT, and forcing a reason on every transfer would only produce a box the
     * user types into just to get through.
     */
    public static SchoolBalanceEntry forQuotaFunding(UUID schoolId, UUID subscriptionId,
            QuotaType quotaType, BigDecimal fundedVnd, BigDecimal balanceAfterVnd, UUID actorId,
            String reason, Instant now) {
        return builder()
            .schoolId(schoolId)
            .subscriptionId(subscriptionId)
            .entryType(SchoolBalanceEntryType.QUOTA_FUNDING)
            .amountVnd(fundedVnd.negate())
            .balanceAfterVnd(balanceAfterVnd)
            .quotaType(quotaType)
            .actorId(actorId)
            .reason(reason)
            .occurredAt(now)
            .build();
    }

    /**
     * Takes a POSITIVE number and the factory flips the sign itself: chk_school_balance_entries_overage_traceable requires
     * amount_vnd &lt; 0, and making the caller remember .negate() leaves a place to forget it. The same constraint requires
     * quotaType/costUsd/fxRateUsed to be NOT NULL and EXACTLY ONE of the two session columns to be set -- gathered here so
     * that "which column is required" is defined in one place, like forTopUp.
     *
     * <p>private, and each of the two public factories above can only set one session column: exposing both parameters
     * would also expose the ability to call it with both null (or both non-null), i.e. to build a row that the database is
     * certain to reject.
     */
    private static SchoolBalanceEntry overageCharge(UUID schoolId, UUID subscriptionId,
            UUID assessmentSessionId, UUID practiceSessionId, QuotaType quotaType, BigDecimal overageVnd,
            BigDecimal balanceAfterVnd, BigDecimal costUsd, BigDecimal fxRateUsed, Instant now) {
        return builder()
            .schoolId(schoolId)
            .subscriptionId(subscriptionId)
            .entryType(SchoolBalanceEntryType.OVERAGE_CHARGE)
            .amountVnd(overageVnd.negate())
            .balanceAfterVnd(balanceAfterVnd)
            .assessmentSessionId(assessmentSessionId)
            .practiceSessionId(practiceSessionId)
            .quotaType(quotaType)
            .costUsd(costUsd)
            .fxRateUsed(fxRateUsed)
            .occurredAt(now)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SchoolBalanceEntry target = new SchoolBalanceEntry();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder subscriptionId(UUID subscriptionId) {
            target.subscriptionId = subscriptionId;
            return this;
        }

        public Builder entryType(SchoolBalanceEntryType entryType) {
            target.entryType = entryType;
            return this;
        }

        public Builder amountVnd(BigDecimal amountVnd) {
            target.amountVnd = amountVnd;
            return this;
        }

        public Builder balanceAfterVnd(BigDecimal balanceAfterVnd) {
            target.balanceAfterVnd = balanceAfterVnd;
            return this;
        }

        public Builder orderId(UUID orderId) {
            target.orderId = orderId;
            return this;
        }

        public Builder assessmentSessionId(UUID assessmentSessionId) {
            target.assessmentSessionId = assessmentSessionId;
            return this;
        }

        public Builder practiceSessionId(UUID practiceSessionId) {
            target.practiceSessionId = practiceSessionId;
            return this;
        }

        public Builder quotaType(QuotaType quotaType) {
            target.quotaType = quotaType;
            return this;
        }

        public Builder costUsd(BigDecimal costUsd) {
            target.costUsd = costUsd;
            return this;
        }

        public Builder fxRateUsed(BigDecimal fxRateUsed) {
            target.fxRateUsed = fxRateUsed;
            return this;
        }

        public Builder actorId(UUID actorId) {
            target.actorId = actorId;
            return this;
        }

        public Builder reason(String reason) {
            target.reason = reason;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            target.occurredAt = occurredAt;
            return this;
        }

        public SchoolBalanceEntry build() {
            return target;
        }

    }
}
