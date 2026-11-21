package com.sep.vox.domain.model.question;

import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class QuestionBank extends BaseModel {
    private UUID languageId; 
    private UUID schoolId;
    private String code;
    private String name;
    private String description;
    private QuestionBankOwnerType ownerType;
    private QuestionBankStatus status;

    public QuestionBank() {}

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

    public static QuestionBank create(UUID languageId, UUID schoolId, String code, String name, String description,
            QuestionBankOwnerType ownerType) {
        return builder()
                .languageId(languageId)
                .schoolId(schoolId)
                .code(code)
                .name(name)
                .description(description)
                .ownerType(ownerType)
                .status(QuestionBankStatus.DRAFT)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<QuestionBank, Builder> {

        private Builder() {
            super(new QuestionBank());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder languageId(UUID languageId) {
            target.languageId = languageId;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder code(String code) {
            target.code = code;
            return this;
        }

        public Builder name(String name) {
            target.name = name;
            return this;
        }

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder ownerType(QuestionBankOwnerType ownerType) {
            target.ownerType = ownerType;
            return this;
        }

        public Builder status(QuestionBankStatus status) {
            target.status = status;
            return this;
        }
    }
}
