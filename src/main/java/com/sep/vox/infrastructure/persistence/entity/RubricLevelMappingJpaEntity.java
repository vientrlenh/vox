package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;


@Entity
@Table(name = "rubric_level_mappings", indexes = {
    @Index(columnList = "rubric_version_id, standard_level_version_id", name = "idx_rubric_level_mappings_version_level", unique = true)
})
public class RubricLevelMappingJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(name = "standard_level_version_id", nullable = false, updatable = false)
    private UUID standardLevelVersionId;

    @Column(name = "score_min", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoreMin;

    @Column(name = "score_max", nullable = false, precision = 6, scale = 2)
    private BigDecimal scoreMax;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected RubricLevelMappingJpaEntity() {}

    public RubricLevelMappingJpaEntity(UUID id, UUID rubricVersionId, UUID standardLevelVersionId, BigDecimal scoreMin,
            BigDecimal scoreMax, String description, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.id = id;
        this.rubricVersionId = rubricVersionId;
        this.standardLevelVersionId = standardLevelVersionId;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public RubricLevelMappingJpaEntity(UUID rubricVersionId, UUID standardLevelVersionId, BigDecimal scoreMin,
            BigDecimal scoreMax, String description, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.rubricVersionId = rubricVersionId;
        this.standardLevelVersionId = standardLevelVersionId;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
        this.description = description;
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

    public UUID getStandardLevelVersionId() {
        return standardLevelVersionId;
    }

    public void setStandardLevelVersionId(UUID standardLevelVersionId) {
        this.standardLevelVersionId = standardLevelVersionId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
