package com.sep.vox.domain.model.assessmentpolicy;

import com.sep.vox.domain.model.BaseModel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AssessmentPolicy extends BaseModel {
    private UUID schoolId;
    private UUID gradeLevelId;
    private UUID schoolGradeId;
    private UUID schoolClassId;
    private UUID languageId;
    private UUID frameworkVersionId;
    private UUID rubricVersionId;
    private UUID targetFrameworkBandId; // which band of the framework is mapped to which grade level or class
    private BigDecimal passingScore; // minimum score to pass; when null the score is not checked
    private AssessmentPolicyStrictness strictness;
    private int version;
    private AssessmentPolicyStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;

    public AssessmentPolicy() {}

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

    public boolean isPublished() {
        return status == AssessmentPolicyStatus.PUBLISHED;
    }

    // Whether it can be used for an assessment
    public boolean isSelectableAt(Instant assessmentOpenAt) {
        return isPublished() && !effectiveFrom.isAfter(assessmentOpenAt) && (effectiveTo == null || !effectiveTo.isBefore(assessmentOpenAt));
    }

    // scope in priority order (class -> grade -> grade level)
    public boolean coversScope(UUID classId, UUID gradeId, UUID gradeLevelId) {
        if (schoolClassId != null) return schoolClassId.equals(classId);
        if (schoolGradeId != null) return schoolGradeId.equals(gradeId);
        return gradeLevelId != null && gradeLevelId.equals(gradeLevelId);
    }

    // a published policy must not be edited
    public void assertMutable() {
        if (isPublished()) {
            throw new IllegalArgumentException("Policy đã được xuất bản, hãy tạo version mới nếu cần sửa");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<AssessmentPolicy, Builder> {

        private Builder() {
            super(new AssessmentPolicy());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder gradeLevelId(UUID gradeLevelId) {
            target.gradeLevelId = gradeLevelId;
            return this;
        }

        public Builder schoolGradeId(UUID schoolGradeId) {
            target.schoolGradeId = schoolGradeId;
            return this;
        }

        public Builder schoolClassId(UUID schoolClassId) {
            target.schoolClassId = schoolClassId;
            return this;
        }

        public Builder languageId(UUID languageId) {
            target.languageId = languageId;
            return this;
        }

        public Builder frameworkVersionId(UUID frameworkVersionId) {
            target.frameworkVersionId = frameworkVersionId;
            return this;
        }

        public Builder rubricVersionId(UUID rubricVersionId) {
            target.rubricVersionId = rubricVersionId;
            return this;
        }

        public Builder targetFrameworkBandId(UUID targetFrameworkBandId) {
            target.targetFrameworkBandId = targetFrameworkBandId;
            return this;
        }

        public Builder passingScore(BigDecimal passingScore) {
            target.passingScore = passingScore;
            return this;
        }

        public Builder strictness(AssessmentPolicyStrictness strictness) {
            target.strictness = strictness;
            return this;
        }

        public Builder version(int version) {
            target.version = version;
            return this;
        }

        public Builder status(AssessmentPolicyStatus status) {
            target.status = status;
            return this;
        }

        public Builder effectiveFrom(Instant effectiveFrom) {
            target.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveTo(Instant effectiveTo) {
            target.effectiveTo = effectiveTo;
            return this;
        }
    }
}
