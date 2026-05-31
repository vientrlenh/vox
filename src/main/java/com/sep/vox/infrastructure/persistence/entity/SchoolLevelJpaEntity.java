package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_levels", indexes = {
    @Index(columnList = "school_id, language_id, framework_id, code", name = "idx_school_levels_school_language_framework_code", unique = true)
})
public class SchoolLevelJpaEntity {
    
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false,
        updatable = false, 
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;
    
    @Column(name = "language_id", nullable = false, updatable = false)
    private UUID languageId;

    @Column(name = "framework_id", nullable = false, updatable = false)
    private UUID frameworkId;

    @Column(name = "code", nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "current_school_level_version_id")
    private UUID currentSchoolLevelVersionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy; 

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    protected SchoolLevelJpaEntity() {}

    public SchoolLevelJpaEntity(UUID id, UUID schoolId, UUID languageId, UUID frameworkId, String code,
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

    public SchoolLevelJpaEntity(UUID schoolId, UUID languageId, UUID frameworkId, String code,
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
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
