package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;


@Entity
@Table(name = "rubric_versions", indexes = {
    @Index(columnList = "rubric_id, code", name = "idx_rubric_versions_rubric_id_code", unique = true),
    @Index(columnList = "rubric_id, version", name = "idx_rubric_versions_rubric_id_version", unique = true),
    @Index(columnList = "status", name = "idx_rubric_versions_status")
}, check = {
    @CheckConstraint(
        name = "chk_rubric_versions_scoring_scale_valid",
        constraint = "scoring_scale_min <= scoring_scale_max"
    ),
    @CheckConstraint(
        name = "chk_rubric_versions_effective_range_valid",
        constraint = "effective_to IS NULL OR effective_from <= effective_to"
    ),
    @CheckConstraint(
        name = "chk_rubric_versions_version_positive",
        constraint = "version > 0"
    )
})
public class RubricVersionJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "rubric_id", nullable = false, updatable = false)
    private UUID rubricId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(name = "chk_rubric_versions_status_valid", constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')")
    })
    private String status;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "scoring_scale_min", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoringScaleMin;

    @Column(name = "scoring_scale_max", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoringScaleMax;

    @Column(name = "total_score_method", nullable = false, length = 30, check = {
        @CheckConstraint(name = "chk_rubric_versions_total_score_method_valid", constraint = "total_score_method IN ('WEIGHTED_AVERAGE', 'SUM')")
    })
    private String totalScoreMethod;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected RubricVersionJpaEntity() {}

    public RubricVersionJpaEntity(UUID id, UUID rubricId, int version, String code, String name, String description, String status, OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo, BigDecimal scoringScaleMin, BigDecimal scoringScaleMax, String totalScoreMethod,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.rubricId = rubricId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.version = version;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.scoringScaleMin = scoringScaleMin;
        this.scoringScaleMax = scoringScaleMax;
        this.totalScoreMethod = totalScoreMethod;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public RubricVersionJpaEntity(UUID rubricId, int version, String code, String name, String description, String status, OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo, BigDecimal scoringScaleMin, BigDecimal scoringScaleMax, String totalScoreMethod,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.rubricId = rubricId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.version = version;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.scoringScaleMin = scoringScaleMin;
        this.scoringScaleMax = scoringScaleMax;
        this.totalScoreMethod = totalScoreMethod;
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

    public UUID getRubricId() {
        return rubricId;
    }

    public void setRubricId(UUID rubricId) {
        this.rubricId = rubricId;
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

    public BigDecimal getScoringScaleMin() {
        return scoringScaleMin;
    }

    public void setScoringScaleMin(BigDecimal scoringScaleMin) {
        this.scoringScaleMin = scoringScaleMin;
    }

    public BigDecimal getScoringScaleMax() {
        return scoringScaleMax;
    }

    public void setScoringScaleMax(BigDecimal scoringScaleMax) {
        this.scoringScaleMax = scoringScaleMax;
    }

    public String getTotalScoreMethod() {
        return totalScoreMethod;
    }

    public void setTotalScoreMethod(String totalScoreMethod) {
        this.totalScoreMethod = totalScoreMethod;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    
}
