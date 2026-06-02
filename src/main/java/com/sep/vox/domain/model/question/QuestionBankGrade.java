package com.sep.vox.domain.model.question;

import java.time.OffsetDateTime;
import java.util.UUID;

public class QuestionBankGrade {
    private UUID id;
    private UUID questionBankId;
    private UUID schoolGradeId;
    private OffsetDateTime attachedAt;
    private UUID attachedBy;

    public QuestionBankGrade() {}

    public QuestionBankGrade(UUID id, UUID questionBankId, UUID schoolGradeId, OffsetDateTime attachedAt,
            UUID attachedBy) {
        this.id = id;
        this.questionBankId = questionBankId;
        this.schoolGradeId = schoolGradeId;
        this.attachedAt = attachedAt;
        this.attachedBy = attachedBy;
    }

    public QuestionBankGrade(UUID questionBankId, UUID schoolGradeId, OffsetDateTime attachedAt, UUID attachedBy) {
        this.questionBankId = questionBankId;
        this.schoolGradeId = schoolGradeId;
        this.attachedAt = attachedAt;
        this.attachedBy = attachedBy;
    }

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

    public OffsetDateTime getAttachedAt() {
        return attachedAt;
    }

    public void setAttachedAt(OffsetDateTime attachedAt) {
        this.attachedAt = attachedAt;
    }

    public UUID getAttachedBy() {
        return attachedBy;
    }

    public void setAttachedBy(UUID attachedBy) {
        this.attachedBy = attachedBy;
    }

    
}
