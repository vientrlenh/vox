package com.sep.vox.domain.model.questionbank;

import java.time.OffsetDateTime;
import java.util.UUID;

public class QuestionBank {
    private UUID id;
    // private UUID gradeId;    // FK chưa link - chờ tạo Grade model
    // private UUID languageId; // FK chưa link - chờ tạo SupportedLanguage model
    private String bankName;
    private String description;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public QuestionBank() {}

    public QuestionBank(UUID id, String bankName, String description, boolean isActive,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.bankName = bankName;
        this.description = description;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public QuestionBank(String bankName, String description, boolean isActive,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.bankName = bankName;
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

    // public UUID getGradeId() {
    //     return gradeId;
    // }
    //
    // public void setGradeId(UUID gradeId) {
    //     this.gradeId = gradeId;
    // }
    //
    // public UUID getLanguageId() {
    //     return languageId;
    // }
    //
    // public void setLanguageId(UUID languageId) {
    //     this.languageId = languageId;
    // }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
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
