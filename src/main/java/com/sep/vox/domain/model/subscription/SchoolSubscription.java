package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class SchoolSubscription {
    private UUID id;
    private UUID schoolId;
    private UUID planId;
    private LocalDate startDate;
    private LocalDate endDate;
    private SubscriptionStatus status;
    private BigDecimal pricePaidSnapshot;
    private OffsetDateTime cancelledAt;
    private OffsetDateTime createdAt;

    public SchoolSubscription() {}

    public SchoolSubscription(UUID id, UUID schoolId, UUID planId, LocalDate startDate, LocalDate endDate,
            SubscriptionStatus status, BigDecimal pricePaidSnapshot, OffsetDateTime cancelledAt, OffsetDateTime createdAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.planId = planId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.pricePaidSnapshot = pricePaidSnapshot;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
    }

    public SchoolSubscription(UUID schoolId, UUID planId, LocalDate startDate, LocalDate endDate,
            SubscriptionStatus status, BigDecimal pricePaidSnapshot, OffsetDateTime cancelledAt, OffsetDateTime createdAt) {
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

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
