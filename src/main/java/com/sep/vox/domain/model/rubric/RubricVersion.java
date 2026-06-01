package com.sep.vox.domain.model.rubric;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;



public class RubricVersion {
    private UUID id;
    private UUID rubricId;
    private String code;
    private String name;
    private String description;
    private int version;
    private RubricStatus status;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private BigDecimal scoringScaleMin;
    private BigDecimal scoringScaleMax;
    private RubricTotalScoreMethod totalScoreMethod;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    
    public RubricVersion() {
    }


    public RubricVersion(UUID id, UUID rubricId, String code, String name, String description, int version, RubricStatus status, OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo, BigDecimal scoringScaleMin, BigDecimal scoringScaleMax,
            RubricTotalScoreMethod totalScoreMethod, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
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


    public RubricVersion(UUID rubricId, String code, String name, String description, int version, RubricStatus status, OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo, BigDecimal scoringScaleMin, BigDecimal scoringScaleMax,
            RubricTotalScoreMethod totalScoreMethod, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
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


    public RubricStatus getStatus() {
        return status;
    }


    public void setStatus(RubricStatus status) {
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


    public RubricTotalScoreMethod getTotalScoreMethod() {
        return totalScoreMethod;
    }


    public void setTotalScoreMethod(RubricTotalScoreMethod totalScoreMethod) {
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
