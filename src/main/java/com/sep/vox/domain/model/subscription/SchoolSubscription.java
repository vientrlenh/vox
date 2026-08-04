package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public class SchoolSubscription {
    private UUID id;
    private UUID schoolId;
    private UUID planId;
    private LocalDate startDate;
    private LocalDate endDate;
    private SubscriptionStatus status;
    private BigDecimal pricePaidSnapshot;
    private Instant cancelledAt;
    private Instant createdAt;
    private Long version;

    public SchoolSubscription() {}

    public SchoolSubscription(UUID id, UUID schoolId, UUID planId, LocalDate startDate, LocalDate endDate,
            SubscriptionStatus status, BigDecimal pricePaidSnapshot, Instant cancelledAt, Instant createdAt,
            Long version) {
        this.id = id;
        this.schoolId = schoolId;
        this.planId = planId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.pricePaidSnapshot = pricePaidSnapshot;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
        this.version = version;
    }

    public SchoolSubscription(UUID schoolId, UUID planId, LocalDate startDate, LocalDate endDate,
            SubscriptionStatus status, BigDecimal pricePaidSnapshot, Instant cancelledAt, Instant createdAt) {
        this.schoolId = schoolId;
        this.planId = planId;
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

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
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
}
