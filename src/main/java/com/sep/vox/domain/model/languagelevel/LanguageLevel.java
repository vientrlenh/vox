package com.sep.vox.domain.model.languagelevel;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.LanguageRank;
import com.sep.vox.domain.valueobject.LevelCode;

public class LanguageLevel {
    private UUID id;
    private UUID schoolId;
    private UUID languageId;
    private LevelCode code;
    private String name;
    private LanguageRank rank;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public LanguageLevel() {}

    public LanguageLevel(UUID id, UUID schoolId, UUID languageId, LevelCode code, String name, LanguageRank rank,
            boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.code = code;
        this.name = name;
        this.rank = rank;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public LanguageLevel(UUID schoolId, UUID languageId, LevelCode code, String name, LanguageRank rank,
            boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.code = code;
        this.name = name;
        this.rank = rank;
        this.isActive = isActive;
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

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getLanguageId() {
        return languageId;
    }

    public void setLanguageId(UUID languageId) {
        this.languageId = languageId;
    }

    public LevelCode getCode() {
        return code;
    }

    public void setCode(LevelCode code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LanguageRank getRank() {
        return rank;
    }

    public void setRank(LanguageRank rank) {
        this.rank = rank;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
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
