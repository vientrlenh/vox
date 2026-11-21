package com.sep.vox.domain.model.question;

import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class Question extends BaseModel {
    private UUID questionBankId;
    private UUID questionTopicId;
    private String code;
    private String instructionText;
    private String questionText;
    private String promptText;
    private String preparationText;
    private QuestionType type;
    private int preparationTimeSeconds;
    private int minResponseSeconds;
    private int maxResponseSeconds;
    private QuestionSharing sharing;
    private UUID sourceQuestionId;
    private boolean locked;
    private QuestionStatus status;
    private QuestionConfidentiality confidentiality;
    private UUID securePoolId;

    public Question() {}

    public UUID getQuestionBankId() {
        return questionBankId;
    }

    public void setQuestionBankId(UUID questionBankId) {
        this.questionBankId = questionBankId;
    }

    public UUID getQuestionTopicId() {
        return questionTopicId;
    }

    public void setQuestionTopicId(UUID questionTopicId) {
        this.questionTopicId = questionTopicId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getInstructionText() {
        return instructionText;
    }

    public void setInstructionText(String instructionText) {
        this.instructionText = instructionText;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getPreparationText() {
        return preparationText;
    }

    public void setPreparationText(String preparationText) {
        this.preparationText = preparationText;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public int getPreparationTimeSeconds() {
        return preparationTimeSeconds;
    }

    public void setPreparationTimeSeconds(int preparationTimeSeconds) {
        this.preparationTimeSeconds = preparationTimeSeconds;
    }

    public int getMinResponseSeconds() {
        return minResponseSeconds;
    }

    public void setMinResponseSeconds(int minResponseSeconds) {
        this.minResponseSeconds = minResponseSeconds;
    }

    public int getMaxResponseSeconds() {
        return maxResponseSeconds;
    }

    public void setMaxResponseSeconds(int maxResponseSeconds) {
        this.maxResponseSeconds = maxResponseSeconds;
    }

    public QuestionSharing getSharing() {
        return sharing;
    }

    public void setSharing(QuestionSharing sharing) {
        this.sharing = sharing;
    }

    public UUID getSourceQuestionId() {
        return sourceQuestionId;
    }

    public void setSourceQuestionId(UUID sourceQuestionId) {
        this.sourceQuestionId = sourceQuestionId;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }

    public QuestionConfidentiality getConfidentiality() {
        return confidentiality;
    }

    public void setConfidentiality(QuestionConfidentiality confidentiality) {
        this.confidentiality = confidentiality;
    }

    public UUID getSecurePoolId() {
        return securePoolId;
    }

    public void setSecurePoolId(UUID securePoolId) {
        this.securePoolId = securePoolId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<Question, Builder> {

        private Builder() {
            super(new Question());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder questionBankId(UUID questionBankId) {
            target.questionBankId = questionBankId;
            return this;
        }

        public Builder questionTopicId(UUID questionTopicId) {
            target.questionTopicId = questionTopicId;
            return this;
        }

        public Builder code(String code) {
            target.code = code;
            return this;
        }

        public Builder instructionText(String instructionText) {
            target.instructionText = instructionText;
            return this;
        }

        public Builder questionText(String questionText) {
            target.questionText = questionText;
            return this;
        }

        public Builder promptText(String promptText) {
            target.promptText = promptText;
            return this;
        }

        public Builder preparationText(String preparationText) {
            target.preparationText = preparationText;
            return this;
        }

        public Builder type(QuestionType type) {
            target.type = type;
            return this;
        }

        public Builder preparationTimeSeconds(int preparationTimeSeconds) {
            target.preparationTimeSeconds = preparationTimeSeconds;
            return this;
        }

        public Builder minResponseSeconds(int minResponseSeconds) {
            target.minResponseSeconds = minResponseSeconds;
            return this;
        }

        public Builder maxResponseSeconds(int maxResponseSeconds) {
            target.maxResponseSeconds = maxResponseSeconds;
            return this;
        }

        public Builder sharing(QuestionSharing sharing) {
            target.sharing = sharing;
            return this;
        }

        public Builder sourceQuestionId(UUID sourceQuestionId) {
            target.sourceQuestionId = sourceQuestionId;
            return this;
        }

        public Builder locked(boolean locked) {
            target.locked = locked;
            return this;
        }

        public Builder status(QuestionStatus status) {
            target.status = status;
            return this;
        }

        public Builder confidentiality(QuestionConfidentiality confidentiality) {
            target.confidentiality = confidentiality;
            return this;
        }

        public Builder securePoolId(UUID securePoolId) {
            target.securePoolId = securePoolId;
            return this;
        }
    }
}
