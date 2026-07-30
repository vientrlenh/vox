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

@Entity
@Table(name = "subscription_request")
public class SubscriptionRequestJpaEntity {

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

    @Column(name = "request_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_subscription_request_request_type_valid",
            constraint = "request_type IN ('REGISTRATION', 'UPGRADE')"
        )
    })
    private String requestType;

    @Column(name = "current_plan_id", updatable = false)
    private UUID currentPlanId;

    @Column(name = "requested_plan_id", nullable = false, updatable = false)
    private UUID requestedPlanId;

    @Column(name = "amount", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_subscription_request_status_valid",
            constraint = "status IN ('PENDING', 'APPROVED', 'REJECTED')"
        )
    })
    private String status;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected SubscriptionRequestJpaEntity() {}

    public SubscriptionRequestJpaEntity(UUID id, UUID schoolId, String requestType, UUID currentPlanId, UUID requestedPlanId,
            BigDecimal amount, String status, Instant submittedAt, UUID reviewedBy, Instant reviewedAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.requestType = requestType;
        this.currentPlanId = currentPlanId;
        this.requestedPlanId = requestedPlanId;
        this.amount = amount;
        this.status = status;
        this.submittedAt = submittedAt;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
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

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public UUID getCurrentPlanId() {
        return currentPlanId;
    }

    public void setCurrentPlanId(UUID currentPlanId) {
        this.currentPlanId = currentPlanId;
    }

    public UUID getRequestedPlanId() {
        return requestedPlanId;
    }

    public void setRequestedPlanId(UUID requestedPlanId) {
        this.requestedPlanId = requestedPlanId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
