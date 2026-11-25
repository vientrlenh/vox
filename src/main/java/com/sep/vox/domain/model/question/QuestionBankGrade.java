package com.sep.vox.domain.model.question;

import java.time.Instant;
import java.util.UUID;

public class QuestionBankGrade {
    private UUID id;
    private UUID questionBankId;
    private UUID schoolGradeId;
    private Instant attachedAt;
    private UUID attachedBy;

    public QuestionBankGrade() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQuestionBankId() {
        return questionBankId;
    }

    public void setQuestionBankId(UUID questionBankId) {
        this.questionBankId = questionBankId;
    }

    public UUID getSchoolGradeId() {
        return schoolGradeId;
    }

    public void setSchoolGradeId(UUID schoolGradeId) {
        this.schoolGradeId = schoolGradeId;
    }

    public Instant getAttachedAt() {
        return attachedAt;
    }

    public void setAttachedAt(Instant attachedAt) {
        this.attachedAt = attachedAt;
    }

    public UUID getAttachedBy() {
        return attachedBy;
    }

    public void setAttachedBy(UUID attachedBy) {
        this.attachedBy = attachedBy;
    }

    public static QuestionBankGrade attached(UUID questionBankId, UUID schoolGradeId, Instant attachedAt,
                                             UUID attachedBy) {
        return builder()
                .questionBankId(questionBankId)
                .schoolGradeId(schoolGradeId)
                .attachedAt(attachedAt)
                .attachedBy(attachedBy)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final QuestionBankGrade target = new QuestionBankGrade();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder questionBankId(UUID questionBankId) {
            target.questionBankId = questionBankId;
            return this;
        }

        public Builder schoolGradeId(UUID schoolGradeId) {
            target.schoolGradeId = schoolGradeId;
            return this;
        }

        public Builder attachedAt(Instant attachedAt) {
            target.attachedAt = attachedAt;
            return this;
        }

        public Builder attachedBy(UUID attachedBy) {
            target.attachedBy = attachedBy;
            return this;
        }

        public QuestionBankGrade build() {
            return target;
        }
    }
}
