package com.sep.vox.domain.model.rubric;

import java.util.UUID;


public class Rubric {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private UUID languageId;
    private UUID frameworkId;
    private RubricOwnerType ownerType;
    private UUID schoolId;
    private UUID currentVersionId;
    
    public Rubric() {
    }

    public Rubric(UUID id, String code, String name, String description, UUID languageId, UUID frameworkId,
            RubricOwnerType ownerType, UUID schoolId, UUID currentVersionId) {
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

    public Rubric(String code, String name, String description, UUID languageId, UUID frameworkId,
            RubricOwnerType ownerType, UUID schoolId, UUID currentVersionId) {
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

    public RubricOwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(RubricOwnerType ownerType) {
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
