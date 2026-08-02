package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PracticeQuestion {

    private UUID id;
    private UUID practiceTopicId;
    private String questionText;
    private String targetCriterionCode;
    private String targetSubAttribute;
    private int difficultyRank;
    private String difficultyFeaturesJson;
    private String evaluationGuideJson;
    private String suggestedIdeasJson;
    private int preparationTimeSeconds;
    private int maxResponseSeconds;
    private int maxFollowupSeconds;
    private Integer vstepPart;
    private String source;
    private int usageCount;
    private boolean active;
    private OffsetDateTime createdAt;

    public PracticeQuestion() {
    }

    public PracticeQuestion(
            UUID id,
            UUID practiceTopicId,
            String questionText,
            String targetCriterionCode,
            String targetSubAttribute,
            int difficultyRank,
            String difficultyFeaturesJson,
            String evaluationGuideJson,
            String suggestedIdeasJson,
            int preparationTimeSeconds,
            int maxResponseSeconds,
            int maxFollowupSeconds,
            Integer vstepPart,
            String source,
            int usageCount,
            boolean active,
            OffsetDateTime createdAt) {
        this.id = id;
        this.practiceTopicId = practiceTopicId;
        this.questionText = questionText;
        this.targetCriterionCode = targetCriterionCode;
        this.targetSubAttribute = targetSubAttribute;
        this.difficultyRank = difficultyRank;
        this.difficultyFeaturesJson = difficultyFeaturesJson;
        this.evaluationGuideJson = evaluationGuideJson;
        this.suggestedIdeasJson = suggestedIdeasJson;
        this.preparationTimeSeconds = preparationTimeSeconds;
        this.maxResponseSeconds = maxResponseSeconds;
        this.maxFollowupSeconds = maxFollowupSeconds;
        this.vstepPart = vstepPart;
        this.source = source;
        this.usageCount = usageCount;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPracticeTopicId() {
        return practiceTopicId;
    }

    public void setPracticeTopicId(UUID practiceTopicId) {
        this.practiceTopicId = practiceTopicId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getTargetCriterionCode() {
        return targetCriterionCode;
    }

    public void setTargetCriterionCode(String targetCriterionCode) {
        this.targetCriterionCode = targetCriterionCode;
    }

    public String getTargetSubAttribute() {
        return targetSubAttribute;
    }

    public void setTargetSubAttribute(String targetSubAttribute) {
        this.targetSubAttribute = targetSubAttribute;
    }

    public int getDifficultyRank() {
        return difficultyRank;
    }

    public void setDifficultyRank(int difficultyRank) {
        this.difficultyRank = difficultyRank;
    }

    public String getDifficultyFeaturesJson() {
        return difficultyFeaturesJson;
    }

    public void setDifficultyFeaturesJson(String difficultyFeaturesJson) {
        this.difficultyFeaturesJson = difficultyFeaturesJson;
    }

    public String getEvaluationGuideJson() {
        return evaluationGuideJson;
    }

    public void setEvaluationGuideJson(String evaluationGuideJson) {
        this.evaluationGuideJson = evaluationGuideJson;
    }

    public String getSuggestedIdeasJson() {
        return suggestedIdeasJson;
    }

    public void setSuggestedIdeasJson(String suggestedIdeasJson) {
        this.suggestedIdeasJson = suggestedIdeasJson;
    }

    public int getPreparationTimeSeconds() {
        return preparationTimeSeconds;
    }

    public void setPreparationTimeSeconds(int preparationTimeSeconds) {
        this.preparationTimeSeconds = preparationTimeSeconds;
    }

    public int getMaxResponseSeconds() {
        return maxResponseSeconds;
    }

    public void setMaxResponseSeconds(int maxResponseSeconds) {
        this.maxResponseSeconds = maxResponseSeconds;
    }

    public int getMaxFollowupSeconds() {
        return maxFollowupSeconds;
    }

    public void setMaxFollowupSeconds(int maxFollowupSeconds) {
        this.maxFollowupSeconds = maxFollowupSeconds;
    }

    public Integer getVstepPart() {
        return vstepPart;
    }

    public void setVstepPart(Integer vstepPart) {
        this.vstepPart = vstepPart;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int spokenSeconds() {
        return maxResponseSeconds + maxFollowupSeconds;
    }

    public int plannedSeconds() {
        return preparationTimeSeconds + spokenSeconds();
    }
}
