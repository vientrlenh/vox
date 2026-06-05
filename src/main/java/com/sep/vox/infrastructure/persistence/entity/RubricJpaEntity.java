package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;


@Entity
@Table(name = "rubrics", indexes = {
    @Index(columnList = "owner_type, school_id, language_id, framework_id, code", name = "idx_rubrics_owner_scope_code", unique = true),
    @Index(columnList = "language_id, framework_id", name = "idx_rubrics_language_framework"),
    @Index(columnList = "current_version_id", name = "idx_rubrics_current_version")
}, check = {
    @CheckConstraint(
        name = "chk_rubrics_owner_school_valid",
        constraint = "(owner_type = 'SYSTEM' AND school_id IS NULL) OR (owner_type = 'SCHOOL' AND school_id IS NOT NULL)"
    )
})

public class RubricJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "language_id", nullable = false, updatable = false)
    private UUID languageId;

    @Column(name = "framework_id", nullable = false)
    private UUID frameworkId;

    @Column(name = "owner_type", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_rubrics_owner_type_valid",
            constraint = "owner_type IN ('SYSTEM', 'SCHOOL')"
        )
    })
    private String ownerType;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    protected RubricJpaEntity() {}

    

    public RubricJpaEntity(UUID id, UUID languageId, UUID frameworkId, String code, String name, String description, 
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



    public RubricJpaEntity(UUID languageId, UUID frameworkId, String code, String name, String description, 
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
