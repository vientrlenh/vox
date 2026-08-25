package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SchoolSubscription {
    private UUID id;
    private UUID schoolId;
    private UUID subscriptionPlanId;
    private Instant startDate;
    private Instant endDate;
    private SchoolSubscriptionStatus status;
    private BigDecimal pricePaidSnapshot;
    private Instant cancelledAt;
    private Instant createdAt;
    private Long version;
    // System Admin cưỡng chế đình chỉ (SUSPENDED) -- khác cancelledAt (chỉ tắt gia hạn). Cả 3 null khi
    // không bị đình chỉ, và bị xóa về null lại khi gỡ đình chỉ (UnsuspendSubscriptionUseCase) -- lịch sử
    // ai đình chỉ/gỡ lúc nào/vì sao được lưu bền ở FinancialEvent, không phải 3 cột này.
    private Instant suspendedAt;
    private String suspendedReason;
    private UUID suspendedBy;

    public SchoolSubscription() {}

    public SchoolSubscription(UUID id, UUID schoolId, UUID subscriptionPlanId, Instant startDate, Instant endDate,
            SchoolSubscriptionStatus status, BigDecimal pricePaidSnapshot, Instant cancelledAt, Instant createdAt,
            Long version, Instant suspendedAt, String suspendedReason, UUID suspendedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionPlanId = subscriptionPlanId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.pricePaidSnapshot = pricePaidSnapshot;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
        this.version = version;
        this.suspendedAt = suspendedAt;
        this.suspendedReason = suspendedReason;
        this.suspendedBy = suspendedBy;
    }

    public SchoolSubscription(UUID schoolId, UUID subscriptionPlanId, Instant startDate, Instant endDate,
            SchoolSubscriptionStatus status, BigDecimal pricePaidSnapshot, Instant cancelledAt, Instant createdAt) {
        this.schoolId = schoolId;
        this.subscriptionPlanId = subscriptionPlanId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.pricePaidSnapshot = pricePaidSnapshot;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
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

    public UUID getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(UUID subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public SchoolSubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolSubscriptionStatus status) {
        this.status = status;
    }

    public BigDecimal getPricePaidSnapshot() {
        return pricePaidSnapshot;
    }

    public void setPricePaidSnapshot(BigDecimal pricePaidSnapshot) {
        this.pricePaidSnapshot = pricePaidSnapshot;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }

    public void setSuspendedAt(Instant suspendedAt) {
        this.suspendedAt = suspendedAt;
    }

    public String getSuspendedReason() {
        return suspendedReason;
    }

    public void setSuspendedReason(String suspendedReason) {
        this.suspendedReason = suspendedReason;
    }

    public UUID getSuspendedBy() {
        return suspendedBy;
    }

    public void setSuspendedBy(UUID suspendedBy) {
        this.suspendedBy = suspendedBy;
    }
}
