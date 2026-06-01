package com.sep.vox.domain.model.languagelevel;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.LevelDifficulty;
import com.sep.vox.domain.valueobject.LevelOrder;
import com.sep.vox.domain.valueobject.LevelVersion;

public class StandardLevelVersion {
    private UUID id;
    private UUID standardLevelId;
    private LevelVersion version;
    private String name;
    private String description;
    private LevelOrder order;
    private LevelDifficulty difficultyMin;
    private LevelDifficulty difficultyMax;
    private LevelStatus status;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public StandardLevelVersion() {}

    public StandardLevelVersion(UUID id, UUID standardLevelId, LevelVersion version, String name, String description,
            LevelOrder order, LevelDifficulty difficultyMin, LevelDifficulty difficultyMax, LevelStatus status,
            OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.standardLevelId = standardLevelId;
        this.version = version;
        this.name = name;
        this.description = description;
        this.order = order;
        this.difficultyMin = difficultyMin;
        this.difficultyMax = difficultyMax;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public StandardLevelVersion(UUID standardLevelId, LevelVersion version, String name, String description,
            LevelOrder order, LevelDifficulty difficultyMin, LevelDifficulty difficultyMax, LevelStatus status,
            OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.standardLevelId = standardLevelId;
        this.version = version;
        this.name = name;
        this.description = description;
        this.order = order;
        this.difficultyMin = difficultyMin;
        this.difficultyMax = difficultyMax;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
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

    public UUID getStandardLevelId() {
        return standardLevelId;
    }

    public void setStandardLevelId(UUID standardLevelId) {
        this.standardLevelId = standardLevelId;
    }

    public LevelVersion getVersion() {
        return version;
    }

    public void setVersion(LevelVersion version) {
        this.version = version;
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

    public LevelOrder getOrder() {
        return order;
    }

    public void setOrder(LevelOrder order) {
        this.order = order;
    }

    public LevelDifficulty getDifficultyMin() {
        return difficultyMin;
    }

    public void setDifficultyMin(LevelDifficulty difficultyMin) {
        this.difficultyMin = difficultyMin;
    }

    public LevelDifficulty getDifficultyMax() {
        return difficultyMax;
    }

    public void setDifficultyMax(LevelDifficulty difficultyMax) {
        this.difficultyMax = difficultyMax;
    }

    public LevelStatus getStatus() {
        return status;
    }

    public void setStatus(LevelStatus status) {
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
