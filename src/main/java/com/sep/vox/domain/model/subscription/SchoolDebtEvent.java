package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Sổ audit "nguyên nhân nợ hạn mức AI" -- mỗi dòng là ĐÚNG 1 bucket quota (GRADING/CLASS_TEST) của 1
 * trường vừa đổi trạng thái (vượt hạn mức lần đầu / vượt trần cảnh báo / hết nợ), kèm session/số USD
 * đã gây ra transition đó. Khác với notification (chỉ tồn tại tạm qua outbox/Kafka), bảng này là ledger
 * append-only để system admin tra lại lịch sử bất kỳ lúc nào -- mirror FinancialEvent/TokenUsageEvent.
 */
public class SchoolDebtEvent {
    private UUID id;
    private UUID schoolId;
    private UUID subscriptionId;
    private SchoolDebtEventType eventType;
    private QuotaType quotaType;
    private UUID triggerExamSessionId;
    private BigDecimal triggerAmountUsd;
    private BigDecimal totalAllocatedUsd;
    private BigDecimal usedQuantityUsd;
    private BigDecimal overageUsd;
    private Instant occurredAt;

    public SchoolDebtEvent() {}

    public SchoolDebtEvent(UUID id, UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType,
            QuotaType quotaType, UUID triggerExamSessionId, BigDecimal triggerAmountUsd,
            BigDecimal totalAllocatedUsd, BigDecimal usedQuantityUsd, BigDecimal overageUsd, Instant occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.quotaType = quotaType;
        this.triggerExamSessionId = triggerExamSessionId;
        this.triggerAmountUsd = triggerAmountUsd;
        this.totalAllocatedUsd = totalAllocatedUsd;
        this.usedQuantityUsd = usedQuantityUsd;
        this.overageUsd = overageUsd;
        this.occurredAt = occurredAt;
    }

    public SchoolDebtEvent(UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType,
            QuotaType quotaType, UUID triggerExamSessionId, BigDecimal triggerAmountUsd,
            BigDecimal totalAllocatedUsd, BigDecimal usedQuantityUsd, BigDecimal overageUsd, Instant occurredAt) {
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.quotaType = quotaType;
        this.triggerExamSessionId = triggerExamSessionId;
        this.triggerAmountUsd = triggerAmountUsd;
        this.totalAllocatedUsd = totalAllocatedUsd;
        this.usedQuantityUsd = usedQuantityUsd;
        this.overageUsd = overageUsd;
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

    public UUID getTriggerExamSessionId() {
        return triggerExamSessionId;
    }

    public void setTriggerExamSessionId(UUID triggerExamSessionId) {
        this.triggerExamSessionId = triggerExamSessionId;
    }

    public BigDecimal getTriggerAmountUsd() {
        return triggerAmountUsd;
    }

    public void setTriggerAmountUsd(BigDecimal triggerAmountUsd) {
        this.triggerAmountUsd = triggerAmountUsd;
    }

    public BigDecimal getTotalAllocatedUsd() {
        return totalAllocatedUsd;
    }

    public void setTotalAllocatedUsd(BigDecimal totalAllocatedUsd) {
        this.totalAllocatedUsd = totalAllocatedUsd;
    }

    public BigDecimal getUsedQuantityUsd() {
        return usedQuantityUsd;
    }

    public void setUsedQuantityUsd(BigDecimal usedQuantityUsd) {
        this.usedQuantityUsd = usedQuantityUsd;
    }

    public BigDecimal getOverageUsd() {
        return overageUsd;
    }

    public void setOverageUsd(BigDecimal overageUsd) {
        this.overageUsd = overageUsd;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
