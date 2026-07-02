package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
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
@Table(name = "questions", indexes = {
    @Index(columnList = "question_topic_id, code", name = "idx_questions_question_topic_code", unique = true),
    @Index(columnList = "type", name = "idx_questions_type"),
    @Index(columnList = "sharing", name = "idx_questions_sharing"),
    @Index(columnList = "source_question_id", name = "idx_questions_source_question")
}, check = {
    @CheckConstraint(
        name = "chk_questions_min_response_seconds_and_max_response_seconds_valid", 
        constraint = "min_response_seconds <= max_response_seconds"
    )
})
public class QuestionJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "question_bank_id", nullable = false, updatable = false)
    private UUID questionBankId;

    @Column(name = "question_topic_id", updatable = false)
    private UUID questionTopicId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "instruction_text", columnDefinition = "TEXT")
    private String instructionText;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "preparation_text", columnDefinition = "TEXT")
    private String preparationText;

    @Column(name = "type", nullable = false, length = 50, check = {
        @CheckConstraint(
            name = "chk_questions_type_valid",
            constraint = "type IN ('READ_ALOUD', 'SHORT_ANSWER', 'LONG_ANSWER', 'OPINION', 'DESCRIPTION')"
        )
    })
    private String type;

    @Column(name = "preparation_time_seconds", nullable = false, check = {
        @CheckConstraint(
            name = "chk_questions_preparation_time_seconds_valid", 
            constraint = "preparation_time_seconds >= 0"
        )
    })
    private int preparationTimeSeconds;

    @Column(name = "min_response_seconds", nullable = false, check = {
        @CheckConstraint(
            name = "chk_questions_min_response_seconds_valid", 
            constraint = "min_response_seconds >= 0"
        )
    })
    private int minResponseSeconds;

    @Column(name = "max_response_seconds", nullable = false, check = {
        @CheckConstraint(
            name = "chk_questions_max_response_seconds_valid", 
            constraint = "max_response_seconds >= 0"
        )
    })
    private int maxResponseSeconds;

    @Column(name = "sharing", length = 100, check = {
        @CheckConstraint(
            name = "chk_questions_sharing_valid", 
            constraint = "sharing IN ('PRIVATE', 'SCHOOL_SHARED')"
        )
    })
    private String sharing;

    @Column(name = "source_question_id")
    private UUID sourceQuestionId;

    @Column(name = "locked", nullable = false)
    private boolean locked;

    @Column(name = "confidentiality", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_questions_confidentiality_valid", 
            constraint = "confidentiality IN ('OPEN', 'EXAM_RESTRICTED', 'RELEASED')"
        )
    })
    private String confidentiality;

    @Column(name = "secure_pool_id")
    private UUID securePoolId;

    @Column(name = "status", nullable = false, check = {
        @CheckConstraint(
            name = "chk_status_valid", 
            constraint = "status IN ('DRAFT', 'SUBMITTED_FOR_REVIEW', 'REVISION_REQUESTED', 'APPROVED', 'REJECTED', 'PUBLISHED', 'ARCHIVED')"
        )
    })
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected QuestionJpaEntity() {}

    public QuestionJpaEntity(UUID id, UUID questionBankId, UUID questionTopicId, String code, String instructionText, String questionText,
            String promptText, String preparationText, String type, int preparationTimeSeconds,
            int minResponseSeconds, int maxResponseSeconds, String sharing, UUID sourceQuestionId, boolean locked, String confidentiality, UUID securePoolId, String status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.questionBankId = questionBankId;
        this.questionTopicId = questionTopicId;
        this.code = code;
        this.instructionText = instructionText;
        this.questionText = questionText;
        this.promptText = promptText;
        this.preparationText = preparationText;
        this.type = type;
        this.preparationTimeSeconds = preparationTimeSeconds;
        this.minResponseSeconds = minResponseSeconds;
        this.maxResponseSeconds = maxResponseSeconds;
        this.sharing = sharing;
        this.sourceQuestionId = sourceQuestionId;
        this.locked = locked;
        this.confidentiality = confidentiality;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getSharing() {
        return sharing;
    }

    public void setSharing(String sharing) {
        this.sharing = sharing;
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

    public UUID getQuestionBankId() {
        return questionBankId;
    }

    public void setQuestionBankId(UUID questionBankId) {
        this.questionBankId = questionBankId;
    }

    public String getConfidentiality() {
        return confidentiality;
    }

    public void setConfidentiality(String confidentiality) {
        this.confidentiality = confidentiality;
    }

    public UUID getSecurePoolId() {
        return securePoolId;
    }

    public void setSecurePoolId(UUID securePoolId) {
        this.securePoolId = securePoolId;
    }

    
}
