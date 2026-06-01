package com.sep.vox.domain.model.schoolclass;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.ClassCode;

public class SchoolClass {
    private UUID id;
    private UUID schoolId;
    private UUID languageId;
    private UUID schoolGradeId;
    private ClassCode code;
    private String name;
    private String description;
    private UUID targetSchoolLevelVersionId;
    private SchoolClassStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public SchoolClass() {}

    public SchoolClass(UUID id, UUID schoolId, UUID languageId, UUID schoolGradeId, ClassCode code, String name, String description, UUID targetSchoolLevelVersionId, SchoolClassStatus status, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.schoolGradeId = schoolGradeId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.targetSchoolLevelVersionId = targetSchoolLevelVersionId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public SchoolClass(UUID schoolId, UUID languageId, UUID schoolGradeId, ClassCode code, String name, String description, UUID targetSchoolLevelVersionId, SchoolClassStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.schoolGradeId = schoolGradeId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.targetSchoolLevelVersionId = targetSchoolLevelVersionId;
        this.status = status;
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

    public UUID getSchoolGradeId() {
        return schoolGradeId;
    }

    public void setSchoolGradeId(UUID schoolGradeId) {
        this.schoolGradeId = schoolGradeId;
    }

    public SchoolClassStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolClassStatus status) {
        this.status = status;
    }

    
}
