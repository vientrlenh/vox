package com.sep.vox.domain.model.languagelevel;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.LevelCode;

public class SchoolLevel {
    private UUID id;
    private UUID schoolId;
    private UUID languageId;
    private UUID frameworkId;
    private LevelCode code;
    private UUID currentSchoolLevelVersionId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public SchoolLevel() {
    }

    public SchoolLevel(UUID id, UUID schoolId, UUID languageId, UUID frameworkId, LevelCode code,
            UUID currentSchoolLevelVersionId, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.frameworkId = frameworkId;
        this.code = code;
        this.currentSchoolLevelVersionId = currentSchoolLevelVersionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    

    public SchoolLevel(UUID schoolId, UUID languageId, UUID frameworkId, LevelCode code,
            UUID currentSchoolLevelVersionId, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.frameworkId = frameworkId;
        this.code = code;
        this.currentSchoolLevelVersionId = currentSchoolLevelVersionId;
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

    public UUID getCurrentSchoolLevelVersionId() {
        return currentSchoolLevelVersionId;
    }

    public void setCurrentSchoolLevelVersionId(UUID currentSchoolLevelVersionId) {
        this.currentSchoolLevelVersionId = currentSchoolLevelVersionId;
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
