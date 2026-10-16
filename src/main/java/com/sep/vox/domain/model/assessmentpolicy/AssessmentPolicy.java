package com.sep.vox.domain.model.assessmentpolicy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.PositiveLong;


public class AssessmentPolicy extends BaseModel {
    private UUID gradeLevelId;
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

    public AssessmentPolicy(UUID gradeLevelId, UUID schoolId, UUID schoolGradeId, UUID schoolClassId,
            UUID learningLanguageId, UUID frameworkVersionId, UUID rubricVersionId, UUID targetFrameworkBandId, PositiveLong version,
            AssessmentPolicyStatus status, LocalDate effectiveFrom, Instant publishedAt, Instant archivedAt, UUID createdBy, UUID publishedBy, UUID archivedBy) {
        this.schoolId = schoolId;
        this.gradeLevelId = gradeLevelId;
        this.schoolGradeId = schoolGradeId;
        this.schoolClassId = schoolClassId;
        this.learningLanguageId = learningLanguageId;
        this.frameworkVersionId = frameworkVersionId;
        this.rubricVersionId = rubricVersionId;
        this.targetFrameworkBandId = targetFrameworkBandId;
        this.version = version;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.publishedAt = publishedAt;
        this.archivedAt = archivedAt;
        this.createdBy = createdBy;
        this.publishedBy = publishedBy;
        this.archivedBy = archivedBy;
    }

    public AssessmentPolicy(Builder builder) {
        super(builder.id, builder.createdAt);
        this.schoolId = builder.schoolId;
        this.gradeLevelId = builder.gradeLevelId;
        this.schoolGradeId = builder.schoolGradeId;
        this.schoolClassId = builder.schoolClassId;
        this.learningLanguageId = builder.learningLanguageId;
        this.frameworkVersionId = builder.frameworkVersionId;
        this.rubricVersionId = builder.rubricVersionId;
        this.targetFrameworkBandId = builder.targetFrameworkBandId;
        this.version = builder.version;
        this.status = builder.status;
        this.effectiveFrom = builder.effectiveFrom;
        this.publishedAt = builder.publishedAt;
        this.archivedAt = builder.archivedAt;
        this.createdBy = builder.createdBy;
        this.publishedBy = builder.publishedBy;
        this.archivedBy = builder.archivedBy;
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

    public PositiveLong getVersion() {
        return version;
    }

    public void setVersion(PositiveLong version) {
        this.version = version;
    }

    public AssessmentPolicyStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentPolicyStatus status) {
        this.status = status;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(UUID publishedBy) {
        this.publishedBy = publishedBy;
    }

    public UUID getArchivedBy() {
        return archivedBy;
    }

    public void setArchivedBy(UUID archivedBy) {
        this.archivedBy = archivedBy;
    }

    public static Builder builder() {
        return new AssessmentPolicy.Builder();
    }

    public boolean isPublished() {
        return status == AssessmentPolicyStatus.PUBLISHED;
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
        private UUID gradeLevelId;
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
        private Instant createdAt;
        private UUID createdBy;
        private UUID publishedBy;
        private UUID archivedBy;

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

        public Builder version(PositiveLong version) {
            this.version = version;
            return this;
        }

        public Builder status(AssessmentPolicyStatus status) {
            this.status = status;
            return this;
        }

        public Builder effectiveFrom(LocalDate effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder publishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder archivedAt(Instant archivedAt) {
            this.archivedAt = archivedAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder publishedBy(UUID publishedBy) {
            this.publishedBy = publishedBy;
            return this;
        }

        public Builder archivedBy(UUID archivedBy) {
            this.archivedBy = archivedBy;
            return this;
        }

        public AssessmentPolicy build() {
            return new AssessmentPolicy(this);
        }
    }
}
