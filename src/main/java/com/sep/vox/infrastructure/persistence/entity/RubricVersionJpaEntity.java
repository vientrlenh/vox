package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Column(name = "version", nullable = false, check = {
        @CheckConstraint(
            name = "chk_rubric_versions_version_positive",
            constraint = "version > 0"
        )
    })
    private int version;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(name = "chk_rubric_versions_status_valid", constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')")
    })
    private String status;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "scoring_scale_min", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoringScaleMin;

    @Column(name = "scoring_scale_max", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoringScaleMax;

    @Column(name = "total_score_method", nullable = false, length = 30, check = {
        @CheckConstraint(name = "chk_rubric_versions_total_score_method_valid", constraint = "total_score_method IN ('WEIGHTED_AVERAGE', 'SUM')")
    })
    private String totalScoreMethod;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Phiên bản gốc mà bản này được sao ra. Không khai khoá ngoại: bản mẫu phía hệ thống có vòng đời
     * riêng và có thể bị lưu trữ hoặc xoá, còn bản sao của trường thì phải sống tiếp bình thường.
     */
    @Column(name = "source_rubric_version_id", updatable = false)
    private UUID sourceRubricVersionId;

    protected RubricVersionJpaEntity() {}

    public RubricVersionJpaEntity(UUID id, UUID rubricId, int version, String code, String name, String description, String status, Instant effectiveFrom,
            Instant effectiveTo, BigDecimal scoringScaleMin, BigDecimal scoringScaleMax, String totalScoreMethod,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
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

    public RubricVersionJpaEntity(UUID rubricId, int version, String code, String name, String description, String status, Instant effectiveFrom,
            Instant effectiveTo, BigDecimal scoringScaleMin, BigDecimal scoringScaleMax, String totalScoreMethod,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
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

    public UUID getSourceRubricVersionId() {
        return sourceRubricVersionId;
    }

    public void setSourceRubricVersionId(UUID sourceRubricVersionId) {
        this.sourceRubricVersionId = sourceRubricVersionId;
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
