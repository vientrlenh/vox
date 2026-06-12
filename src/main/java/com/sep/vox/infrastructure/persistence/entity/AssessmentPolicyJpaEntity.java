package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.math.BigDecimal;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;


@Entity
@Table(name = "assessment_policies", indexes = {
    @Index(columnList = "school_id, school_grade_level_id, school_grade_id, school_class_id, language_id, framework_version_id, version",
        name = "idx_assessment_policies_scope_version", unique = true),
    @Index(columnList = "school_id, school_grade_level_id, school_grade_id, language_id, framework_version_id, status",
        name = "idx_assessment_policies_grade_status"),
    @Index(columnList = "rubric_version_id", name = "idx_assessment_policies_rubric_version"),
    @Index(columnList = "target_framework_band_id", name = "idx_assessment_policies_target_band"),
    @Index(columnList = "minimum_framework_band_id", name = "idx_assessment_policies_minimum_band")
}, check = {
    @CheckConstraint(
        name = "chk_assessment_policies_effective_range_valid",
        constraint = "effective_to IS NULL OR effective_from <= effective_to"
    )
})
public class AssessmentPolicyJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "school_grade_level_id", nullable = false, updatable = false)
    private UUID schoolGradeLevelId;

    @Column(name = "school_grade_id", updatable = false)
    private UUID schoolGradeId;

    @Column(name = "school_class_id", updatable = false)
    private UUID schoolClassId;

    @Column(name = "language_id", nullable = false, updatable = false)
    private UUID languageId;

    @Column(name = "framework_version_id", nullable = false, updatable = false)
    private UUID frameworkVersionId;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(name = "target_framework_band_id", nullable = false)
    private UUID targetFrameworkBandId;

    @Column(name = "minimum_framework_band_id", nullable = false)
    private UUID minimumFrameworkBandId;

    @Column(name = "passing_score", precision = 6, scale = 2, check = {
        @CheckConstraint(
            name = "chk_assessment_policies_passing_score_non_negative",
            constraint = "passing_score IS NULL OR passing_score >= 0"
        )
    })
    private BigDecimal passingScore;

    @Column(name = "strictness", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_policies_strictness_valid",
            constraint = "strictness IN ('LENIENT', 'STANDARD', 'STRICT')"
        )
    })
    private String strictness;

    @Column(name = "version", nullable = false, check = {
        @CheckConstraint(
            name = "chk_assessment_policies_version_positive",
            constraint = "version > 0"
        )
    })
    private int version;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_policies_status_valid",
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')"
        ),
    })
    private String status;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected AssessmentPolicyJpaEntity() {}

    public AssessmentPolicyJpaEntity(UUID id, UUID schoolId, UUID schoolGradeLevelId, UUID schoolGradeId,
            UUID schoolClassId, UUID languageId, UUID frameworkVersionId, UUID rubricVersionId,
            UUID targetFrameworkBandId, UUID minimumFrameworkBandId, BigDecimal passingScore, String strictness,
            int version, String status, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo,
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

    public String getStrictness() {
        return strictness;
    }

    public void setStrictness(String strictness) {
        this.strictness = strictness;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
