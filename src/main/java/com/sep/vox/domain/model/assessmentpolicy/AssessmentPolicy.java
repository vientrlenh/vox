package com.sep.vox.domain.model.assessmentpolicy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.domain.valueobject.PositiveLong;


public class AssessmentPolicy extends BaseModel {
    private UUID schoolId;
    private UUID schoolGradeId;
    private UUID schoolClassId;
    private UUID learningLanguageId;
    private UUID frameworkVersionId;
    private UUID rubricVersionId;
    private UUID targetFrameworkBandId;
    private PositiveLong version;
    private AssessmentPolicyStatus status;
    private LocalDate effectiveFrom;
    private Instant publishedAt;
    private Instant archivedAt;
    private UUID createdBy;
    private UUID publishedBy;
    private UUID archivedBy;

    protected AssessmentPolicy() {}

    public AssessmentPolicy(UUID id, UUID schoolId, UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId,
            UUID learningLanguageId, UUID frameworkVersionId, UUID rubricVersionId, UUID targetFrameworkBandId,
            PositiveDecimal passingScore, AssessmentPolicyStrictness strictness, PositiveInteger version,
            AssessmentPolicyStatus status, Instant effectiveFrom, Instant effectiveTo) {
        this.id = id;
        this.schoolId = schoolId;
        this.gradeLevelId = gradeLevelId;
        this.schoolGradeId = schoolGradeId;
        this.schoolClassId = schoolClassId;
        this.learningLanguageId = learningLanguageId;
        this.frameworkVersionId = frameworkVersionId;
        this.rubricVersionId = rubricVersionId;
        this.targetFrameworkBandId = targetFrameworkBandId;
        this.passingScore = passingScore;
        this.strictness = strictness;
        this.version = version;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public AssessmentPolicy(Builder builder) {
        this.id = builder.id;
        this.schoolId = builder.schoolId;
        this.gradeLevelId = builder.gradeLevelId;
        this.schoolGradeId = builder.schoolGradeId;
        this.schoolClassId = builder.schoolClassId;
        this.learningLanguageId = builder.learningLanguageId;
        this.frameworkVersionId = builder.frameworkVersionId;
        this.rubricVersionId = builder.rubricVersionId;
        this.targetFrameworkBandId = builder.targetFrameworkBandId;
        this.passingScore = builder.passingScore;
        this.strictness = builder.strictness;
        this.version = builder.version;
        this.status = builder.status;
        this.effectiveFrom = builder.effectiveFrom;
        this.effectiveTo = builder.effectiveTo;
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

    public UUID getLearningLanguageId() {
        return learningLanguageId;
    }

    public void setLearningLanguageId(UUID learningLanguageId) {
        this.learningLanguageId = learningLanguageId;
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

    public PositiveDecimal getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(PositiveDecimal passingScore) {
        this.passingScore = passingScore;
    }

    public AssessmentPolicyStrictness getStrictness() {
        return strictness;
    }

    public void setStrictness(AssessmentPolicyStrictness strictness) {
        this.strictness = strictness;
    }

    public PositiveInteger getVersion() {
        return version;
    }

    public void setVersion(PositiveInteger version) {
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

    public static Builder builder() {
        return new AssessmentPolicy.Builder();
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
            throw new IllegalArgumentException("Policy đã được xuất bản, hãy tạo version mới nếu cần sửa");
        }
    }

    public static class Builder {
        private UUID id;
        private UUID schoolId;
        private UUID gradeLevelId;
        private UUID schoolGradeId;
        private UUID schoolClassId;
        private UUID learningLanguageId;
        private UUID frameworkVersionId;
        private UUID rubricVersionId;
        private UUID targetFrameworkBandId;
        private PositiveDecimal passingScore;
        private AssessmentPolicyStrictness strictness;
        private PositiveInteger version;
        private AssessmentPolicyStatus status;
        private Instant effectiveFrom;
        private Instant effectiveTo;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            this.schoolId = schoolId;
            return this;
        }

        public Builder gradeLevelId(UUID gradeLevelId) {
            this.gradeLevelId = gradeLevelId;
            return this;
        }

        public Builder schoolGradeId(UUID schoolGradeId) {
            this.schoolGradeId = schoolGradeId;
            return this;
        }

        public Builder schoolClassId(UUID schoolClassId) {
            this.schoolClassId = schoolClassId;
            return this;
        }

        public Builder learningLanguageId(UUID learningLanguageId) {
            this.learningLanguageId = learningLanguageId;
            return this;
        }

        public Builder frameworkVersionId(UUID frameworkVersionId) {
            this.frameworkVersionId = frameworkVersionId;
            return this;
        }

        public Builder rubricVersionId(UUID rubricVersionId) {
            this.rubricVersionId = rubricVersionId;
            return this;
        }

        public Builder targetFrameworkBandId(UUID targetFrameworkBandId) {
            this.targetFrameworkBandId = targetFrameworkBandId;
            return this;
        }

        public Builder passingScore(PositiveDecimal passingScore) {
            this.passingScore = passingScore;
            return this;
        }

        public Builder strictness(AssessmentPolicyStrictness strictness) {
            this.strictness = strictness;
            return this;
        }

        public Builder version(PositiveInteger version) {
            this.version = version;
            return this;
        }

        public Builder status(AssessmentPolicyStatus status) {
            this.status = status;
            return this;
        }

        public Builder effectiveFrom(Instant effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveTo(Instant effectiveTo) {
            this.effectiveTo = effectiveTo;
            return this;
        }

        public AssessmentPolicy build() {
            return new AssessmentPolicy(this);
        }
    }
}
