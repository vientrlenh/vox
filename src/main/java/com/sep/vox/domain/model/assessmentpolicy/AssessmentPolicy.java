package com.sep.vox.domain.model.assessmentpolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


public class AssessmentPolicy {
    private UUID id;
    private UUID schoolId;
    private UUID gradeLevelId;
    private UUID schoolGradeId;
    private UUID schoolClassId;
    private UUID languageId;
    private UUID frameworkVersionId;
    private UUID rubricVersionId;
    private UUID targetFrameworkBandId; // band nào ở khung được map với khối hoặc lớp nào
    private BigDecimal passingScore; // điểm tối thiểu để qua, nếu null thì không check điểm
    private AssessmentPolicyStrictness strictness;
    private int version;
    private AssessmentPolicyStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public AssessmentPolicy() {}

    public AssessmentPolicy(UUID id, UUID schoolId, UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId,
            UUID languageId, UUID frameworkVersionId, UUID rubricVersionId, UUID targetFrameworkBandId,
            BigDecimal passingScore, AssessmentPolicyStrictness strictness, int version,
            AssessmentPolicyStatus status, Instant effectiveFrom, Instant effectiveTo,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.gradeLevelId = gradeLevelId;
        this.schoolGradeId = schoolGradeId;
        this.schoolClassId = schoolClassId;
        this.languageId = languageId;
        this.frameworkVersionId = frameworkVersionId;
        this.rubricVersionId = rubricVersionId;
        this.targetFrameworkBandId = targetFrameworkBandId;
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

    public AssessmentPolicy(UUID schoolId, UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId,
            UUID languageId, UUID frameworkVersionId, UUID rubricVersionId, UUID targetFrameworkBandId,
            BigDecimal passingScore, AssessmentPolicyStrictness strictness, int version,
            AssessmentPolicyStatus status, Instant effectiveFrom, Instant effectiveTo,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.schoolId = schoolId;
        this.gradeLevelId = gradeLevelId;
        this.schoolGradeId = schoolGradeId;
        this.schoolClassId = schoolClassId;
        this.languageId = languageId;
        this.frameworkVersionId = frameworkVersionId;
        this.rubricVersionId = rubricVersionId;
        this.targetFrameworkBandId = targetFrameworkBandId;
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

    public UUID getGradeLevelId() {
        return gradeLevelId;
    }

    public void setGradeLevelId(UUID gradeLevelId) {
        this.gradeLevelId = gradeLevelId;
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

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
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

    public boolean isPublished() {
        return status == AssessmentPolicyStatus.PUBLISHED;
    }

    // có dùng được cho exam không
    public boolean isSelectableAt(Instant examOpenAt) {
        return isPublished() && !effectiveFrom.isAfter(examOpenAt) && (effectiveTo == null || !effectiveTo.isBefore(examOpenAt));
    }

    // scope theo thứ tự ưu tiên (class -> grade -> grade level)
    public boolean coversScope(UUID classId, UUID gradeId, UUID gradeLevelId) {
        if (schoolClassId != null) return schoolClassId.equals(classId);
        if (schoolGradeId != null) return schoolGradeId.equals(gradeId);
        return gradeLevelId != null && gradeLevelId.equals(gradeLevelId);
    }

    // không sửa khi đã published
    public void assertMutable() {
        if (isPublished()) {
            throw new IllegalStateException("Policy đã được xuất bản, hãy tạo version mới nếu cần sửa");
        }
    }
    
}
