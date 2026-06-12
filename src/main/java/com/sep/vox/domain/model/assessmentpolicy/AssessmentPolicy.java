package com.sep.vox.domain.model.assessmentpolicy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


public class AssessmentPolicy {
    private UUID id;
    private UUID schoolId;
    private UUID schoolGradeLevelId;
    private UUID schoolGradeId;
    private UUID schoolClassId;
    private UUID languageId;
    private UUID frameworkVersionId;
    private UUID rubricVersionId;
    private UUID targetFrameworkBandId; // band nào ở khung được map với khối hoặc lớp nào
    private UUID minimumFrameworkBandId; // phải đạt tối thiểu band X mới qua
    private BigDecimal passingScore;
    private AssessmentPolicyStrictness strictness;
    private int version;
    private AssessmentPolicyStatus status;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public AssessmentPolicy() {}

    public AssessmentPolicy(UUID id, UUID schoolId, UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId,
            UUID languageId, UUID frameworkVersionId, UUID rubricVersionId, UUID targetFrameworkBandId,
            UUID minimumFrameworkBandId, BigDecimal passingScore, AssessmentPolicyStrictness strictness, int version,
            AssessmentPolicyStatus status, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.schoolGradeLevelId = schoolGradeLevelId;
        this.schoolGradeId = schoolGradeId;
        this.schoolClassId = schoolClassId;
        this.languageId = languageId;
        this.frameworkVersionId = frameworkVersionId;
        this.rubricVersionId = rubricVersionId;
        this.targetFrameworkBandId = targetFrameworkBandId;
        this.minimumFrameworkBandId = minimumFrameworkBandId;
        this.passingScore = passingScore;
        this.strictness = strictness;
        this.version = version;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public AssessmentPolicy(UUID schoolId, UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId,
            UUID languageId, UUID frameworkVersionId, UUID rubricVersionId, UUID targetFrameworkBandId,
            UUID minimumFrameworkBandId, BigDecimal passingScore, AssessmentPolicyStrictness strictness, int version,
            AssessmentPolicyStatus status, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.schoolId = schoolId;
        this.schoolGradeLevelId = schoolGradeLevelId;
        this.schoolGradeId = schoolGradeId;
        this.schoolClassId = schoolClassId;
        this.languageId = languageId;
        this.frameworkVersionId = frameworkVersionId;
        this.rubricVersionId = rubricVersionId;
        this.targetFrameworkBandId = targetFrameworkBandId;
        this.minimumFrameworkBandId = minimumFrameworkBandId;
        this.passingScore = passingScore;
        this.strictness = strictness;
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

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getSchoolGradeLevelId() {
        return schoolGradeLevelId;
    }

    public void setSchoolGradeLevelId(UUID schoolGradeLevelId) {
        this.schoolGradeLevelId = schoolGradeLevelId;
    }

    public UUID getSchoolGradeId() {
        return schoolGradeId;
    }

    public void setSchoolGradeId(UUID schoolGradeId) {
        this.schoolGradeId = schoolGradeId;
    }

    public UUID getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(UUID schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

    public UUID getLanguageId() {
        return languageId;
    }

    public void setLanguageId(UUID languageId) {
        this.languageId = languageId;
    }

    public UUID getFrameworkVersionId() {
        return frameworkVersionId;
    }

    public void setFrameworkVersionId(UUID frameworkVersionId) {
        this.frameworkVersionId = frameworkVersionId;
    }

    public UUID getRubricVersionId() {
        return rubricVersionId;
    }

    public void setRubricVersionId(UUID rubricVersionId) {
        this.rubricVersionId = rubricVersionId;
    }

    public UUID getTargetFrameworkBandId() {
        return targetFrameworkBandId;
    }

    public void setTargetFrameworkBandId(UUID targetFrameworkBandId) {
        this.targetFrameworkBandId = targetFrameworkBandId;
    }

    public UUID getMinimumFrameworkBandId() {
        return minimumFrameworkBandId;
    }

    public void setMinimumFrameworkBandId(UUID minimumFrameworkBandId) {
        this.minimumFrameworkBandId = minimumFrameworkBandId;
    }

    public BigDecimal getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(BigDecimal passingScore) {
        this.passingScore = passingScore;
    }

    public AssessmentPolicyStrictness getStrictness() {
        return strictness;
    }

    public void setStrictness(AssessmentPolicyStrictness strictness) {
        this.strictness = strictness;
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
