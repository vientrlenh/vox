package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class SubscriptionRequest {
    private UUID id;
    private UUID schoolId;
    private RequestType requestType;
    private UUID currentPlanId;
    private UUID requestedPlanId;
    private BigDecimal amount;
    private RequestStatus status;
    private OffsetDateTime submittedAt;
    private UUID reviewedBy;
    private OffsetDateTime reviewedAt;

    public SubscriptionRequest() {}

    public SubscriptionRequest(UUID id, UUID schoolId, RequestType requestType, UUID currentPlanId, UUID requestedPlanId,
            BigDecimal amount, RequestStatus status, OffsetDateTime submittedAt, UUID reviewedBy, OffsetDateTime reviewedAt) {
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

    public SubscriptionRequest(UUID schoolId, RequestType requestType, UUID currentPlanId, UUID requestedPlanId,
            BigDecimal amount, RequestStatus status, OffsetDateTime submittedAt, UUID reviewedBy, OffsetDateTime reviewedAt) {
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

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
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

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
