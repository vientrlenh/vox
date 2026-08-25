package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Append-only: mọi cột đều {@code updatable = false}. Sửa một bút toán đã ghi là sai nghiệp vụ --
 * muốn điều chỉnh thì ghi thêm một dòng ADJUSTMENT.
 */
@Entity
@Table(name = "school_balance_entries")
public class SchoolBalanceEntryJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "subscription_id", updatable = false)
    private UUID subscriptionId;

    @Column(name = "entry_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_balance_entries_entry_type_valid",
            constraint = "entry_type IN ('TOP_UP', 'OVERAGE_CHARGE', 'REFUND', 'ADJUSTMENT')"
        )
    })
    private String entryType;

    // numeric(18,6) chứ không phải (15,0) như tiền mặt qua cổng: một lượt luyện nói có thể chỉ tốn
    // vài phần trăm đồng, làm tròn về số nguyên là mất trắng khoản trừ đó.
    @Column(name = "amount_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal amountVnd;

    @Column(name = "balance_after_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal balanceAfterVnd;

    @Column(name = "order_id", updatable = false)
    private UUID orderId;

    @Column(name = "exam_session_id", updatable = false)
    private UUID examSessionId;

    @Column(name = "quota_type", updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_balance_entries_quota_type_valid",
            constraint = "quota_type IS NULL OR quota_type IN ('GRADING', 'CLASS_TEST', 'PRACTICE')"
        )
    })
    private String quotaType;

    // Giữ USD: đây là hóa đơn nhà cung cấp tính cho mình, đối soát ngược với ai_usage_records và là
    // đầu vào của QuotaPricingCalibrationService. Quy sang VND ở đây sẽ làm rate calibrate trôi theo
    // tỷ giá thay vì theo chi phí thật.
    @Column(name = "cost_usd", updatable = false, precision = 18, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "fx_rate_used", updatable = false, precision = 12, scale = 4)
    private BigDecimal fxRateUsed;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "reason", updatable = false, length = 2048)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected SchoolBalanceEntryJpaEntity() {}

    public SchoolBalanceEntryJpaEntity(UUID id, UUID schoolId, UUID subscriptionId, String entryType,
            BigDecimal amountVnd, BigDecimal balanceAfterVnd, UUID orderId, UUID examSessionId, String quotaType,
            BigDecimal costUsd, BigDecimal fxRateUsed, UUID actorId, String reason, Instant occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.entryType = entryType;
        this.amountVnd = amountVnd;
        this.balanceAfterVnd = balanceAfterVnd;
        this.orderId = orderId;
        this.examSessionId = examSessionId;
        this.quotaType = quotaType;
        this.costUsd = costUsd;
        this.fxRateUsed = fxRateUsed;
        this.actorId = actorId;
        this.reason = reason;
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

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
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

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public void setExamSessionId(UUID examSessionId) {
        this.examSessionId = examSessionId;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
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
}
