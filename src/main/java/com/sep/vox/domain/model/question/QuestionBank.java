package com.sep.vox.domain.model.question;

import java.time.OffsetDateTime;
import java.util.UUID;

public class QuestionBank {
    private UUID id;
    private UUID languageId; 
    private UUID schoolId;
    private String code;
    private String name;
    private String description;
    private QuestionBankOwnerType ownerType;
    private QuestionBankStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public QuestionBank() {}

    public QuestionBank(UUID id, UUID languageId, UUID schoolId, String code, String name, String description, QuestionBankOwnerType ownerType, QuestionBankStatus status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.languageId = languageId;
        this.schoolId = schoolId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.ownerType = ownerType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public QuestionBank(UUID languageId, UUID schoolId, String code, String name, String description, 
            QuestionBankOwnerType ownerType, QuestionBankStatus status, 
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.languageId = languageId;
        this.schoolId = schoolId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.ownerType = ownerType;
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

    public UUID getLanguageId() {
        return languageId;
    }
    
    public void setLanguageId(UUID languageId) {
        this.languageId = languageId;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public QuestionBankStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionBankStatus status) {
        this.status = status;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public QuestionBankOwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(QuestionBankOwnerType ownerType) {
        this.ownerType = ownerType;
    }

    public static QuestionBank create(UUID languageId, UUID schoolId, String code, String name, String description, QuestionBankOwnerType ownerType, 
    OffsetDateTime now, UUID createdBy) {
        return new QuestionBank(
            languageId, 
            schoolId, 
            code, 
            name, 
            description, 
            ownerType, 
            QuestionBankStatus.DRAFT, 
            now, 
            now, 
            createdBy, 
            createdBy
        );
    }
}
