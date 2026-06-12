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
@Table(name = "rubric_criterions", indexes = {
    @Index(columnList = "rubric_version_id, code", name = "idx_rubric_criterions_version_code", unique = true),
    @Index(columnList = "rubric_version_id, framework_criterion_id", name = "idx_rubric_criterions_version_framework_criterion", unique = true),
    @Index(columnList = "framework_criterion_id", name = "idx_rubric_criterions_framework_criterion")
}, check = {
    @CheckConstraint(
        name = "chk_rubric_criterions_score_range_valid",
        constraint = "min_score <= max_score"
    )

})
public class RubricCriterionJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(name = "framework_criterion_id", nullable = false, updatable = false)
    private UUID frameworkCriterionId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "examples_json", columnDefinition = "TEXT")
    private String examplesJson;

    @Column(name = "weight", nullable = false, precision = 6, scale = 2, check = {
        @CheckConstraint(
            name = "chk_rubric_criterions_weight_non_negative",
            constraint = "weight >= 0"
        )
    })
    private BigDecimal weight;

    @Column(name = "min_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal minScore;

    @Column(name = "max_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "criterion_order", nullable = false, check = {
        @CheckConstraint(
            name = "chk_rubric_criterions_order_positive",
            constraint = "criterion_order > 0"
        )
    })
    private int order;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected RubricCriterionJpaEntity() {}

    public RubricCriterionJpaEntity(UUID id, UUID rubricVersionId, UUID frameworkCriterionId, String code,
            String name, String description, String examplesJson, BigDecimal weight, BigDecimal minScore,
            BigDecimal maxScore, int order, boolean isRequired, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.rubricVersionId = rubricVersionId;
        this.frameworkCriterionId = frameworkCriterionId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.examplesJson = examplesJson;
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

    public String getExamplesJson() {
        return examplesJson;
    }

    public void setExamplesJson(String examplesJson) {
        this.examplesJson = examplesJson;
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
