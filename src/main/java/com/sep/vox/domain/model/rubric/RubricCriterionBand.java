package com.sep.vox.domain.model.rubric;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.rubriccriterionbandexample.RubricCriterionBandExamples;
import com.sep.vox.domain.valueobject.rubriccriterionsignal.RubricCriterionSignals;


public class RubricCriterionBand {
    private UUID id;
    private UUID criterionId;
    private String code;
    private BigDecimal scoreMin;
    private BigDecimal scoreMax;
    private String descriptor;
    private RubricCriterionSignals positiveSignals;
    private RubricCriterionSignals negativeSignals;
    private RubricCriterionBandExamples examples;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;


    public RubricCriterionBand() {
    }



    public RubricCriterionBand(UUID id, UUID criterionId, String code, BigDecimal scoreMin, BigDecimal scoreMax,
            String descriptor, RubricCriterionSignals positiveSignals, RubricCriterionSignals negativeSignals,
            RubricCriterionBandExamples examples, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.id = id;
        this.criterionId = criterionId;
        this.code = code;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
        this.descriptor = descriptor;
        this.positiveSignals = positiveSignals;
        this.negativeSignals = negativeSignals;
        this.examples = examples;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }



    public RubricCriterionBand(UUID criterionId, String code, BigDecimal scoreMin, BigDecimal scoreMax,
            String descriptor, RubricCriterionSignals positiveSignals, RubricCriterionSignals negativeSignals,
            RubricCriterionBandExamples examples, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.criterionId = criterionId;
        this.code = code;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
        this.descriptor = descriptor;
        this.positiveSignals = positiveSignals;
        this.negativeSignals = negativeSignals;
        this.examples = examples;
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



    public RubricCriterionSignals getPositiveSignals() {
        return positiveSignals;
    }



    public void setPositiveSignals(RubricCriterionSignals positiveSignals) {
        this.positiveSignals = positiveSignals;
    }



    public RubricCriterionSignals getNegativeSignals() {
        return negativeSignals;
    }



    public void setNegativeSignals(RubricCriterionSignals negativeSignals) {
        this.negativeSignals = negativeSignals;
    }



    public RubricCriterionBandExamples getExamples() {
        return examples;
    }



    public void setExamples(RubricCriterionBandExamples examples) {
        this.examples = examples;
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
