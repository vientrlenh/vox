package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Sổ audit "nguyên nhân nợ hạn mức AI" -- mỗi dòng là ĐÚNG 1 ví quota (EXAM/PRACTICE) của 1 trường
 * vừa đổi trạng thái (rơi vào nợ / vượt trần cảnh báo / hết nợ), kèm session và số tiền VND đã gây ra
 * transition đó. Khác với notification (chỉ tồn tại tạm qua outbox/Kafka), bảng này là ledger
 * append-only để system admin tra lại lịch sử bất kỳ lúc nào -- mirror FinancialEvent/TokenUsageEvent.
 *
 * <p>Mọi cột tiền ở đây là VND, cùng đơn vị với school_balances/school_subscription_quota_records --
 * KHÔNG còn USD. Tệ gốc của nhà cung cấp chỉ còn sống ở ai_usage_records.cost_usd và
 * school_balance_entries.cost_usd, là hai chỗ thật sự cần đối soát ngược với hóa đơn Azure.
 */
public class SchoolDebtEvent {
    private UUID id;
    private UUID schoolId;
    private UUID subscriptionId;
    private SchoolDebtEventType eventType;
    private QuotaType quotaType;
    private UUID triggerExamSessionId;
    private BigDecimal triggerAmountVnd;
    private BigDecimal totalAllocatedVnd;
    private BigDecimal usedAmountVnd;
    private BigDecimal overageVnd;
    private Instant occurredAt;

    public SchoolDebtEvent() {}

    public SchoolDebtEvent(UUID id, UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType,
            QuotaType quotaType, UUID triggerExamSessionId, BigDecimal triggerAmountVnd,
            BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd, BigDecimal overageVnd, Instant occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.quotaType = quotaType;
        this.triggerExamSessionId = triggerExamSessionId;
        this.triggerAmountVnd = triggerAmountVnd;
        this.totalAllocatedVnd = totalAllocatedVnd;
        this.usedAmountVnd = usedAmountVnd;
        this.overageVnd = overageVnd;
        this.occurredAt = occurredAt;
    }

    public SchoolDebtEvent(UUID schoolId, UUID subscriptionId, SchoolDebtEventType eventType,
            QuotaType quotaType, UUID triggerExamSessionId, BigDecimal triggerAmountVnd,
            BigDecimal totalAllocatedVnd, BigDecimal usedAmountVnd, BigDecimal overageVnd, Instant occurredAt) {
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.quotaType = quotaType;
        this.triggerExamSessionId = triggerExamSessionId;
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

    public UUID getTriggerExamSessionId() {
        return triggerExamSessionId;
    }

    public void setTriggerExamSessionId(UUID triggerExamSessionId) {
        this.triggerExamSessionId = triggerExamSessionId;
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
