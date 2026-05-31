package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;


@Entity
@Table(name = "rubrics", indexes = {
    @Index(columnList = "owner_type, school_id, language_id, framework_id, code", name = "idx_rubrics_owner_scope_code", unique = true)
})

public class RubricJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "code", nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "language_id", nullable = false, updatable = false)
    private UUID languageId;

    @Column(name = "framework_id", nullable = false, updatable = false)
    private UUID frameworkId;

    @Column(name = "owner_type", nullable = false, length = 20)
    private String ownerType;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    protected RubricJpaEntity() {}

    

    public RubricJpaEntity(UUID id, String code, String name, String description, UUID languageId, UUID frameworkId,
            String ownerType, UUID schoolId, UUID currentVersionId) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.languageId = languageId;
        this.frameworkId = frameworkId;
        this.ownerType = ownerType;
        this.schoolId = schoolId;
        this.currentVersionId = currentVersionId;
    }



    public RubricJpaEntity(String code, String name, String description, UUID languageId, UUID frameworkId,
            String ownerType, UUID schoolId, UUID currentVersionId) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.languageId = languageId;
        this.frameworkId = frameworkId;
        this.ownerType = ownerType;
        this.schoolId = schoolId;
        this.currentVersionId = currentVersionId;
    }



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(UUID currentVersionId) {
        this.currentVersionId = currentVersionId;
    }

    
}
