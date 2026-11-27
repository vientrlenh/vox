package com.sep.vox.domain.model.question;

import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class QuestionTopic extends BaseModel {
    private UUID questionBankId;
    private String code;
    private String name;
    private String description;
    private QuestionTopicStatus status;

    public QuestionTopic() {}

    public UUID getQuestionBankId() {
        return questionBankId;
    }

    public void setQuestionBankId(UUID questionBankId) {
        this.questionBankId = questionBankId;
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

    public QuestionTopicStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionTopicStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return this.status == QuestionTopicStatus.PUBLISHED;
    }

    public static QuestionTopic draft(UUID questionBankId, String code, String name, String description) {
        return builder()
                .questionBankId(questionBankId)
                .code(code)
                .name(name)
                .description(description)
                .status(QuestionTopicStatus.DRAFT)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<QuestionTopic, Builder> {

        private Builder() {
            super(new QuestionTopic());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder questionBankId(UUID questionBankId) {
            target.questionBankId = questionBankId;
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

        public Builder status(QuestionTopicStatus status) {
            target.status = status;
            return this;
        }
    }
}
