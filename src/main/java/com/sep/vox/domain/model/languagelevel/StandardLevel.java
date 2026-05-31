package com.sep.vox.domain.model.languagelevel;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.LevelCode;

public class StandardLevel {
    private UUID id;
    private UUID languageId;
    private UUID frameworkId;
    private LevelCode code;
    private UUID currentVersionId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public StandardLevel() {}

    public StandardLevel(UUID id, UUID languageId, UUID frameworkId, LevelCode code, UUID currentVersionId,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.languageId = languageId;
        this.frameworkId = frameworkId;
        this.code = code;
        this.currentVersionId = currentVersionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public StandardLevel(UUID languageId, UUID frameworkId, LevelCode code, UUID currentVersionId,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.languageId = languageId;
        this.frameworkId = frameworkId;
        this.code = code;
        this.currentVersionId = currentVersionId;
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

    public UUID getLanguageId() {
        return languageId;
    }

    public void setLanguageId(UUID languageId) {
        this.languageId = languageId;
    }

    public UUID getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(UUID frameworkId) {
        this.frameworkId = frameworkId;
    }

    public LevelCode getCode() {
        return code;
    }

    public void setCode(LevelCode code) {
        this.code = code;
    }

    public UUID getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(UUID currentVersionId) {
        this.currentVersionId = currentVersionId;
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
