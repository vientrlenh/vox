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
    // Kỳ ĐĂNG KÝ đã phát sinh khoản nợ, không phải gói. Tên cũ subscriptionPlanId là sai và sai một
    // chiều nguy hiểm: cột DB là subscription_id, JpaEntity cũng là subscriptionId, nên chỉ mỗi domain
    // model gọi khác -- đọc code ở tầng này sẽ tưởng đang cầm id của gói.
    private UUID subscriptionId;
    private SchoolDebtEventType eventType;
    /** null với CLEARED: hết nợ là sự kiện cấp TRƯỜNG, số dư không chia theo ví hạn mức nào. */
    private QuotaType quotaType;
    /** LOCKED/CAP_EXCEEDED: ĐÚNG MỘT trong hai cột session dưới đây được set. CLEARED: cả hai null. */
    private UUID triggerAssessmentSessionId;
    private UUID triggerPracticeSessionId;
    private BigDecimal triggerAmountVnd;
    private BigDecimal totalAllocatedVnd;
    private BigDecimal usedAmountVnd;
    private BigDecimal overageVnd;
    private Instant occurredAt;

    public SchoolDebtEvent() {}

    public SchoolDebtEvent(UUID id, UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType,
            QuotaType quotaType, UUID triggerAssessmentSessionId, UUID triggerPracticeSessionId,
            BigDecimal triggerAmountVnd,
            BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd, BigDecimal overageVnd, Instant occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.quotaType = quotaType;
        this.triggerAssessmentSessionId = triggerAssessmentSessionId;
        this.triggerPracticeSessionId = triggerPracticeSessionId;
        this.triggerAmountVnd = triggerAmountVnd;
        this.totalAllocatedVnd = totalAllocatedVnd;
        this.usedAmountVnd = usedAmountVnd;
        this.overageVnd = overageVnd;
        this.occurredAt = occurredAt;
    }

    public SchoolDebtEvent(UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType,
            QuotaType quotaType, UUID triggerAssessmentSessionId, UUID triggerPracticeSessionId,
            BigDecimal triggerAmountVnd,
            BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd, BigDecimal overageVnd, Instant occurredAt) {
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.quotaType = quotaType;
        this.triggerAssessmentSessionId = triggerAssessmentSessionId;
        this.triggerPracticeSessionId = triggerPracticeSessionId;
        this.triggerAmountVnd = triggerAmountVnd;
        this.totalAllocatedVnd = totalAllocatedVnd;
        this.usedAmountVnd = usedAmountVnd;
        this.overageVnd = overageVnd;
        this.occurredAt = occurredAt;
    }

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
}
