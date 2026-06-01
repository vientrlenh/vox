package com.sep.vox.domain.model.assessmentpolicy;

import java.time.OffsetDateTime;
import java.util.UUID;


public class AssessmentPolicy {
    private UUID id;
    private AssessmentPolicyOwnerType ownerType;
    private UUID schoolId;
    private UUID rubricVersionId;
    private int version;
    private AssessmentPolicyStatus status;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    
    public AssessmentPolicy() {
    }


    public AssessmentPolicy(UUID id, AssessmentPolicyOwnerType ownerType, UUID schoolId, UUID rubricVersionId,
            int version, AssessmentPolicyStatus status, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.ownerType = ownerType;
        this.schoolId = schoolId;
        this.rubricVersionId = rubricVersionId;
        this.version = version;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }


    public AssessmentPolicy(AssessmentPolicyOwnerType ownerType, UUID schoolId, UUID rubricVersionId, int version,
            AssessmentPolicyStatus status, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.ownerType = ownerType;
        this.schoolId = schoolId;
        this.rubricVersionId = rubricVersionId;
        this.version = version;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }


    public UUID getId() {
        return id;
    }


    public void setId(UUID id) {
        this.id = id;
    }


    public AssessmentPolicyOwnerType getOwnerType() {
        return ownerType;
    }


    public void setOwnerType(AssessmentPolicyOwnerType ownerType) {
        this.ownerType = ownerType;
    }


    public UUID getSchoolId() {
        return schoolId;
    }


    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }


    public UUID getRubricVersionId() {
        return rubricVersionId;
    }


    public void setRubricVersionId(UUID rubricVersionId) {
        this.rubricVersionId = rubricVersionId;
    }


    public int getVersion() {
        return version;
    }


    public void setVersion(int version) {
        this.version = version;
    }


    public AssessmentPolicyStatus getStatus() {
        return status;
    }


    public void setStatus(AssessmentPolicyStatus status) {
        this.status = status;
    }


    public OffsetDateTime getEffectiveFrom() {
        return effectiveFrom;
    }


    public void setEffectiveFrom(OffsetDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }


    public OffsetDateTime getEffectiveTo() {
        return effectiveTo;
    }


    public void setEffectiveTo(OffsetDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }


    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }


    public UUID getCreatedBy() {
        return createdBy;
    }


    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }


    public UUID getUpdatedBy() {
        return updatedBy;
    }


    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    
}
