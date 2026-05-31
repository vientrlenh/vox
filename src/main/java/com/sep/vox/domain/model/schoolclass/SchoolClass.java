package com.sep.vox.domain.model.schoolclass;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.ClassCode;

public class SchoolClass {
    private UUID id;
    private UUID schoolId;
    private UUID languageId;
    private ClassCode code;
    private String name;
    private String description;
    private UUID targetSchoolLevelVersionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public SchoolClass() {}

    public SchoolClass(UUID id, UUID schoolId, UUID languageId, ClassCode code, String name, String description, UUID targetSchoolLevelVersionId,
            LocalDate startDate, LocalDate endDate, boolean isActive, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.targetSchoolLevelVersionId = targetSchoolLevelVersionId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public SchoolClass(UUID schoolId, UUID languageId, ClassCode code, String name, String description, UUID targetSchoolLevelVersionId, LocalDate startDate,
            LocalDate endDate, boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.targetSchoolLevelVersionId = targetSchoolLevelVersionId;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public ClassCode getCode() {
        return code;
    }

    public void setCode(ClassCode code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getTargetSchoolLevelVersionId() {
        return targetSchoolLevelVersionId;
    }

    public void setTargetSchoolLevelVersionId(UUID targetSchoolLevelVersionId) {
        this.targetSchoolLevelVersionId = targetSchoolLevelVersionId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    
}
