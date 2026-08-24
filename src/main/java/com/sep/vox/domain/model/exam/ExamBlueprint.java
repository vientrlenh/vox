package com.sep.vox.domain.model.exam;

import java.time.Instant;
import java.util.UUID;

public class ExamBlueprint {
    private UUID id;
    private UUID schoolId;
    private UUID languageId;
    private UUID gradeLevelId;
    private String code;
    private String name;
    private String description;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public ExamBlueprint() {}

    public ExamBlueprint(UUID id, UUID schoolId, UUID languageId, UUID gradeLevelId, String code, String name,
            String description, boolean isActive, Instant createdAt, Instant updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.gradeLevelId = gradeLevelId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public ExamBlueprint(UUID schoolId, UUID languageId, UUID gradeLevelId, String code, String name,
            String description, boolean isActive, Instant createdAt, Instant updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.gradeLevelId = gradeLevelId;
        this.code = code;
        this.name = name;
        this.description = description;
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

    public UUID getGradeLevelId() {
        return gradeLevelId;
    }

    public void setGradeLevelId(UUID gradeLevelId) {
        this.gradeLevelId = gradeLevelId;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
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
