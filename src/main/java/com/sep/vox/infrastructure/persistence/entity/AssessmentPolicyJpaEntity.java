package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
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
    @Index(columnList = "target_framework_band_id", name = "idx_assessment_policies_target_band")
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

    @Column(name = "school_id", updatable = false)
    private UUID schoolId;

    @Column(name = "school_grade_level_id", updatable = false)
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
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected AssessmentPolicyJpaEntity() {}

    public AssessmentPolicyJpaEntity(UUID id, UUID schoolId, UUID schoolGradeLevelId, UUID schoolGradeId,
            UUID schoolClassId, UUID languageId, UUID frameworkVersionId, UUID rubricVersionId,
            UUID targetFrameworkBandId, BigDecimal passingScore, String strictness,
            int version, String status, Instant effectiveFrom, Instant effectiveTo,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.schoolGradeLevelId = schoolGradeLevelId;
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

    
}
