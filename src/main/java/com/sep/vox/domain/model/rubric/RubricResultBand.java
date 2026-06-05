package com.sep.vox.domain.model.rubric;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class RubricResultBand {
    private UUID id;
    private UUID rubricVersionId;
    private UUID frameworkResultBandId;
    private String code;
    private String name;
    private String description;
    private BigDecimal mappedScoreMin;
    private BigDecimal mappedScoreMax;
    private int order;
    private Boolean isPassing;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public RubricResultBand() {}

    public RubricResultBand(UUID id, UUID rubricVersionId, UUID frameworkResultBandId, String code, String name,
            String description, BigDecimal mappedScoreMin, BigDecimal mappedScoreMax, int order, Boolean isPassing,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.rubricVersionId = rubricVersionId;
        this.frameworkResultBandId = frameworkResultBandId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.mappedScoreMin = mappedScoreMin;
        this.mappedScoreMax = mappedScoreMax;
        this.order = order;
        this.isPassing = isPassing;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public RubricResultBand(UUID rubricVersionId, UUID frameworkResultBandId, String code, String name,
            String description, BigDecimal mappedScoreMin, BigDecimal mappedScoreMax, int order, Boolean isPassing,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.rubricVersionId = rubricVersionId;
        this.frameworkResultBandId = frameworkResultBandId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.mappedScoreMin = mappedScoreMin;
        this.mappedScoreMax = mappedScoreMax;
        this.order = order;
        this.isPassing = isPassing;
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

    public UUID getFrameworkResultBandId() {
        return frameworkResultBandId;
    }

    public void setFrameworkResultBandId(UUID frameworkResultBandId) {
        this.frameworkResultBandId = frameworkResultBandId;
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

    public BigDecimal getMappedScoreMin() {
        return mappedScoreMin;
    }

    public void setMappedScoreMin(BigDecimal mappedScoreMin) {
        this.mappedScoreMin = mappedScoreMin;
    }

    public BigDecimal getMappedScoreMax() {
        return mappedScoreMax;
    }

    public void setMappedScoreMax(BigDecimal mappedScoreMax) {
        this.mappedScoreMax = mappedScoreMax;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Boolean getIsPassing() {
        return isPassing;
    }

    public void setIsPassing(Boolean isPassing) {
        this.isPassing = isPassing;
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
