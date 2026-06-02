package com.sep.vox.domain.model.question;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Question {
    private UUID id;
    private UUID questionTopicId;
    private String code;
    private String instructionText;
    private String questionText;
    private String promptText;
    private String preparationText;
    private UUID standardLevelVersionId;
    private UUID schoolLevelVersionId;
    private QuestionType type;
    private int preparationTimeSeconds;
    private int minResponseSeconds;
    private int maxResponseSeconds;
    private QuestionScope scope;
    private QuestionVisibility visibility;
    private UUID sourceQuestionId;
    private boolean locked;
    private QuestionStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public Question() {}

    public Question(UUID id, UUID questionTopicId, String code, String instructionText, String questionText, String promptText,
            String preparationText, UUID standardLevelVersionId, UUID schoolLevelVersionId, QuestionType type,
            int preparationTimeSeconds, int minResponseSeconds, int maxResponseSeconds, QuestionScope scope,
            QuestionVisibility visibility, UUID sourceQuestionId, boolean locked, QuestionStatus status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.questionTopicId = questionTopicId;
        this.code = code;
        this.instructionText = instructionText;
        this.questionText = questionText;
        this.promptText = promptText;
        this.preparationText = preparationText;
        this.standardLevelVersionId = standardLevelVersionId;
        this.schoolLevelVersionId = schoolLevelVersionId;
        this.type = type;
        this.preparationTimeSeconds = preparationTimeSeconds;
        this.minResponseSeconds = minResponseSeconds;
        this.maxResponseSeconds = maxResponseSeconds;
        this.scope = scope;
        this.visibility = visibility;
        this.sourceQuestionId = sourceQuestionId;
        this.locked = locked;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public Question(UUID questionTopicId, String code, String instructionText, String questionText, String promptText,
            String preparationText, UUID standardLevelVersionId, UUID schoolLevelVersionId, QuestionType type,
            int preparationTimeSeconds, int minResponseSeconds, int maxResponseSeconds, QuestionScope scope,
            QuestionVisibility visibility, UUID sourceQuestionId, boolean locked, QuestionStatus status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.questionTopicId = questionTopicId;
        this.code = code;
        this.instructionText = instructionText;
        this.questionText = questionText;
        this.promptText = promptText;
        this.preparationText = preparationText;
        this.standardLevelVersionId = standardLevelVersionId;
        this.schoolLevelVersionId = schoolLevelVersionId;
        this.type = type;
        this.preparationTimeSeconds = preparationTimeSeconds;
        this.minResponseSeconds = minResponseSeconds;
        this.maxResponseSeconds = maxResponseSeconds;
        this.scope = scope;
        this.visibility = visibility;
        this.sourceQuestionId = sourceQuestionId;
        this.locked = locked;
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

    public UUID getStandardLevelVersionId() {
        return standardLevelVersionId;
    }

    public void setStandardLevelVersionId(UUID standardLevelVersionId) {
        this.standardLevelVersionId = standardLevelVersionId;
    }

    public UUID getSchoolLevelVersionId() {
        return schoolLevelVersionId;
    }

    public void setSchoolLevelVersionId(UUID schoolLevelVersionId) {
        this.schoolLevelVersionId = schoolLevelVersionId;
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

    public QuestionScope getScope() {
        return scope;
    }

    public void setScope(QuestionScope scope) {
        this.scope = scope;
    }

    public QuestionVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(QuestionVisibility visibility) {
        this.visibility = visibility;
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

    public static Question create(UUID questionTopicId, String code, String instructionText, String questionText, String promptText,
        String preparationText, UUID standardLevelVersionId, UUID schoolLevelVersionId, QuestionType type,
        int preparationTimeSeconds, int minResponseSeconds, int maxResponseSeconds, QuestionScope scope,
        QuestionVisibility visibility, UUID sourceQuestionId, boolean locked, OffsetDateTime now, UUID createdBy) {
        return new Question(
            questionTopicId, 
            code, 
            instructionText, 
            questionText, 
            promptText, 
            preparationText, 
            standardLevelVersionId, 
            schoolLevelVersionId, 
            type, 
            preparationTimeSeconds, 
            minResponseSeconds, 
            maxResponseSeconds, 
            scope,
            visibility,
            sourceQuestionId,
            locked,
            QuestionStatus.DRAFT, 
            now, 
            now, 
            createdBy, 
            createdBy
        );
    }
}
