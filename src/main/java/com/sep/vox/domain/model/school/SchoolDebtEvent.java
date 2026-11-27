package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * The audit ledger of "the cause of AI quota debt" -- each row is EXACTLY 1 quota wallet (ASSESSMENT/PRACTICE) of 1 school
 * that just changed state (fell into debt / crossed the warning ceiling / came out of debt), with the session and the VND amount that caused
 * that transition. Unlike a notification (which only exists temporarily through the outbox/Kafka), this table is an
 * append-only ledger so the system admin can look up the history at any time -- a mirror of SchoolSubscriptionEvent.
 *
 * <p>Every money column here is VND, the same unit as school_balances/school_subscription_quota_records --
 * NO MORE USD. The provider's original currency only lives on in ai_usage_records.cost_usd and
 * school_balance_entries.cost_usd, the two places that really need reconciling back against the Azure invoice.
 */
public class SchoolDebtEvent {
    private UUID id;
    private UUID schoolId;
    // The SUBSCRIPTION period that incurred the debt, not the plan. The old name subscriptionPlanId was wrong, and wrong in
    // a dangerous direction: the DB column is subscription_id and the JpaEntity is also subscriptionId, so only the domain
    // model named it differently -- reading code at this layer would make one think it is holding a plan id.
    private UUID subscriptionId;
    private SchoolDebtEventType eventType;
    /** null for CLEARED: clearing debt is a SCHOOL-level event, the balance is not split by any quota wallet. */
    private QuotaType quotaType;
    /** LOCKED/CAP_EXCEEDED: EXACTLY ONE of the two session columns below is set. CLEARED: both are null. */
    private UUID triggerAssessmentSessionId;
    private UUID triggerPracticeSessionId;
    private BigDecimal triggerAmountVnd;
    private BigDecimal totalAllocatedVnd;
    private BigDecimal usedAmountVnd;
    private BigDecimal overageVnd;
    private Instant occurredAt;

    public SchoolDebtEvent() {}



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

    public SchoolDebtEventType getEventType() {
        return eventType;
    }

    public void setEventType(SchoolDebtEventType eventType) {
        this.eventType = eventType;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public UUID getTriggerAssessmentSessionId() {
        return triggerAssessmentSessionId;
    }

    public void setTriggerAssessmentSessionId(UUID triggerAssessmentSessionId) {
        this.triggerAssessmentSessionId = triggerAssessmentSessionId;
    }

    public UUID getTriggerPracticeSessionId() {
        return triggerPracticeSessionId;
    }

    public void setTriggerPracticeSessionId(UUID triggerPracticeSessionId) {
        this.triggerPracticeSessionId = triggerPracticeSessionId;
    }

    public BigDecimal getTriggerAmountVnd() {
        return triggerAmountVnd;
    }

    public void setTriggerAmountVnd(BigDecimal triggerAmountVnd) {
        this.triggerAmountVnd = triggerAmountVnd;
    }

    public BigDecimal getTotalAllocatedVnd() {
        return totalAllocatedVnd;
    }

    public void setTotalAllocatedVnd(BigDecimal totalAllocatedVnd) {
        this.totalAllocatedVnd = totalAllocatedVnd;
    }

    public BigDecimal getUsedAmountVnd() {
        return usedAmountVnd;
    }

    public void setUsedAmountVnd(BigDecimal usedAmountVnd) {
        this.usedAmountVnd = usedAmountVnd;
    }

    public BigDecimal getOverageVnd() {
        return overageVnd;
    }

    public void setOverageVnd(BigDecimal overageVnd) {
        this.overageVnd = overageVnd;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public static SchoolDebtEvent recorded(
            UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType, QuotaType quotaType,
            UUID triggerAssessmentSessionId, UUID triggerPracticeSessionId, BigDecimal triggerAmountVnd,
            BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd, BigDecimal overageVnd, Instant occurredAt) {
        return builder()
                .schoolId(schoolId)
                .subscriptionId(subscriptionId)
                .eventType(eventType)
                .quotaType(quotaType)
                .triggerAssessmentSessionId(triggerAssessmentSessionId)
                .triggerPracticeSessionId(triggerPracticeSessionId)
                .triggerAmountVnd(triggerAmountVnd)
                .totalAllocatedVnd(totalAllocatedVnd)
                .usedAmountVnd(usedAmountVnd)
                .overageVnd(overageVnd)
                .occurredAt(occurredAt)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SchoolDebtEvent target = new SchoolDebtEvent();

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

        public Builder eventType(SchoolDebtEventType eventType) {
            target.eventType = eventType;
            return this;
        }

        public Builder quotaType(QuotaType quotaType) {
            target.quotaType = quotaType;
            return this;
        }

        public Builder triggerAssessmentSessionId(UUID triggerAssessmentSessionId) {
            target.triggerAssessmentSessionId = triggerAssessmentSessionId;
            return this;
        }

        public Builder triggerPracticeSessionId(UUID triggerPracticeSessionId) {
            target.triggerPracticeSessionId = triggerPracticeSessionId;
            return this;
        }

        public Builder triggerAmountVnd(BigDecimal triggerAmountVnd) {
            target.triggerAmountVnd = triggerAmountVnd;
            return this;
        }

        public Builder totalAllocatedVnd(BigDecimal totalAllocatedVnd) {
            target.totalAllocatedVnd = totalAllocatedVnd;
            return this;
        }

        public Builder usedAmountVnd(BigDecimal usedAmountVnd) {
            target.usedAmountVnd = usedAmountVnd;
            return this;
        }

        public Builder overageVnd(BigDecimal overageVnd) {
            target.overageVnd = overageVnd;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            target.occurredAt = occurredAt;
            return this;
        }

        public SchoolDebtEvent build() {
            return target;
        }

    }
}
