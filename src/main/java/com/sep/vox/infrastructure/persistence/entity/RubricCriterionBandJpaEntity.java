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
@Table(name = "rubric_criterion_bands", indexes = {
    @Index(columnList = "criterion_id, code", name = "idx_rubric_criterion_bands_criterion_code", unique = true)
}, check = {
    @CheckConstraint(
        name = "chk_rubric_criterion_bands_score_range_valid",
        constraint = "score_min <= score_max"
    )
})

public class RubricCriterionBandJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "criterion_id", nullable = false, updatable = false)
    private UUID criterionId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "score_min", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoreMin;

    @Column(name = "score_max", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoreMax;

    @Column(name = "descriptor", nullable = false, length = 4096)
    private String descriptor;

    @Column(name = "positive_signals_json", nullable = false, columnDefinition = "TEXT")
    private String positiveSignalsJson;

    @Column(name = "negative_signals_json", nullable = false, columnDefinition = "TEXT")
    private String negativeSignalsJson;

    @Column(name = "examples_json", nullable = false, columnDefinition = "TEXT")
    private String examplesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected RubricCriterionBandJpaEntity() {}

    public RubricCriterionBandJpaEntity(UUID id, UUID criterionId, String code, BigDecimal scoreMin,
            BigDecimal scoreMax, String descriptor, String positiveSignalsJson, String negativeSignalsJson,
            String examplesJson, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.criterionId = criterionId;
        this.code = code;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
        this.descriptor = descriptor;
        this.positiveSignalsJson = positiveSignalsJson;
        this.negativeSignalsJson = negativeSignalsJson;
        this.examplesJson = examplesJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public RubricCriterionBandJpaEntity(UUID criterionId, String code, BigDecimal scoreMin, BigDecimal scoreMax,
            String descriptor, String positiveSignalsJson, String negativeSignalsJson, String examplesJson,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.criterionId = criterionId;
        this.code = code;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
        this.descriptor = descriptor;
        this.positiveSignalsJson = positiveSignalsJson;
        this.negativeSignalsJson = negativeSignalsJson;
        this.examplesJson = examplesJson;
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

    public UUID getCriterionId() {
        return criterionId;
    }

    public void setCriterionId(UUID criterionId) {
        this.criterionId = criterionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getScoreMin() {
        return scoreMin;
    }

    public void setScoreMin(BigDecimal scoreMin) {
        this.scoreMin = scoreMin;
    }

    public BigDecimal getScoreMax() {
        return scoreMax;
    }

    public void setScoreMax(BigDecimal scoreMax) {
        this.scoreMax = scoreMax;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getPositiveSignalsJson() {
        return positiveSignalsJson;
    }

    public void setPositiveSignalsJson(String positiveSignalsJson) {
        this.positiveSignalsJson = positiveSignalsJson;
    }

    public String getNegativeSignalsJson() {
        return negativeSignalsJson;
    }

    public void setNegativeSignalsJson(String negativeSignalsJson) {
        this.negativeSignalsJson = negativeSignalsJson;
    }

    public String getExamplesJson() {
        return examplesJson;
    }

    public void setExamplesJson(String examplesJson) {
        this.examplesJson = examplesJson;
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
