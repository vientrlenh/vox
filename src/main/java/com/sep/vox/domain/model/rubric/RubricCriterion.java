package com.sep.vox.domain.model.rubric;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;


public class RubricCriterion {
    private UUID id;
    private UUID rubricVersionId;
    private UUID frameworkCriterionId;
    private String code;
    private String name;
    private String description;
    private RubricCriterionExamples examples;
    private BigDecimal weight;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private int order;
    private boolean isRequired;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public RubricCriterion() {}

    public RubricCriterion(UUID id, UUID rubricVersionId, UUID frameworkCriterionId, String code, String name,
            String description, RubricCriterionExamples examples, BigDecimal weight, BigDecimal minScore, BigDecimal maxScore, int order,
            boolean isRequired, Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.rubricVersionId = rubricVersionId;
        this.frameworkCriterionId = frameworkCriterionId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.examples = examples;
        this.weight = weight;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.order = order;
        this.isRequired = isRequired;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public RubricCriterion(UUID rubricVersionId, UUID frameworkCriterionId, String code, String name,
            String description, RubricCriterionExamples examples, BigDecimal weight, BigDecimal minScore, BigDecimal maxScore, int order,
            boolean isRequired, Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.rubricVersionId = rubricVersionId;
        this.frameworkCriterionId = frameworkCriterionId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.examples = examples;
        this.weight = weight;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.order = order;
        this.isRequired = isRequired;
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

    public UUID getRubricVersionId() {
        return rubricVersionId;
    }

    public void setRubricVersionId(UUID rubricVersionId) {
        this.rubricVersionId = rubricVersionId;
    }

    public UUID getFrameworkCriterionId() {
        return frameworkCriterionId;
    }

    public void setFrameworkCriterionId(UUID frameworkCriterionId) {
        this.frameworkCriterionId = frameworkCriterionId;
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

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public void setMinScore(BigDecimal minScore) {
        this.minScore = minScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public RubricCriterionExamples getExamples() {
        return examples;
    }

    public void setExamples(RubricCriterionExamples examples) {
        this.examples = examples;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean isRequired) {
        this.isRequired = isRequired;
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
