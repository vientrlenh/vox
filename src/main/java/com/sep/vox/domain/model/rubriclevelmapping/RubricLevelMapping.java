package com.sep.vox.domain.model.rubriclevelmapping;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


public class RubricLevelMapping {
    private UUID id;
    private UUID rubricVersionId;
    private UUID standardLevelVersionId;
    private BigDecimal scoreMin;
    private BigDecimal scoreMax;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    
    public RubricLevelMapping() {
    }

    public RubricLevelMapping(UUID id, UUID rubricVersionId, UUID standardLevelVersionId, BigDecimal scoreMin,
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

    public RubricLevelMapping(UUID rubricVersionId, UUID standardLevelVersionId, BigDecimal scoreMin,
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
