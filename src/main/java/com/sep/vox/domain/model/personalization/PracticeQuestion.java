package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

public class PracticeQuestion {

    private UUID id;
    private UUID practiceTopicId;
    private String questionText;
    private String targetCriterionCode;
    private String targetSubAttribute;

    /**
     * The tense this question forces the student to answer in -- PRESENT/PAST/FUTURE/PERFECT/CONDITIONAL, see
     * {@code TensePolicy}. {@code null} means UNKNOWN (a question written before this column existed),
     * not "no tense is forced"; the question-selection ladder treats null as usable.
     */
    private String targetTense;
    private int difficultyRank;
    private String difficultyFeaturesJson;
    private String evaluationGuideJson;
    private String suggestedIdeasJson;
    private String questionType;
    private int maxResponseSeconds;
    private int minResponseSeconds;
    private Integer vstepPart;
    private String source;
    private int usageCount;
    private boolean active;
    private Instant createdAt;

    public PracticeQuestion() {
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

    public String getTargetTense() {
        return targetTense;
    }

    public void setTargetTense(String targetTense) {
        this.targetTense = targetTense;
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

    /** SHORT_ANSWER | LONG_ANSWER | DESCRIPTION | OPINION -- xem V15__personalize.sql. */
    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public int getMaxResponseSeconds() {
        return maxResponseSeconds;
    }

    public void setMaxResponseSeconds(int maxResponseSeconds) {
        this.maxResponseSeconds = maxResponseSeconds;
    }

    public int getMinResponseSeconds() {
        return minResponseSeconds;
    }

    public void setMinResponseSeconds(int minResponseSeconds) {
        this.minResponseSeconds = minResponseSeconds;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * The speaking ceiling of the WHOLE question, including the follow-up chain.
     *
     * It used to be {@code maxResponseSeconds + maxFollowupSeconds}, but the follow-up column is a dead
     * column (the LLM fills it in although the prompt never describes it, so the whole table holds 0) -- that sum always equals
     * maxResponseSeconds. It is now stated directly instead of adding a term that is always 0.
     *
     * A follow-up has no budget of its own because SignalNode already accumulates seconds across every turn of the
     * same question; what it lacks is the FLOOR that says when enough is enough, and that is {@link #getMinResponseSeconds()}.
     */
    public int spokenSeconds() {
        return maxResponseSeconds;
    }

    public static PracticeQuestion generated(UUID id, UUID practiceTopicId, String questionText,
                                             String targetCriterionCode, String targetSubAttribute,
                                             String targetTense, int difficultyRank, String difficultyFeaturesJson,
                                             String evaluationGuideJson, String suggestedIdeasJson,
                                             String questionType, int maxResponseSeconds, int minResponseSeconds,
                                             Integer vstepPart, Instant createdAt) {
        return builder()
                .id(id)
                .practiceTopicId(practiceTopicId)
                .questionText(questionText)
                .targetCriterionCode(targetCriterionCode)
                .targetSubAttribute(targetSubAttribute)
                .targetTense(targetTense)
                .difficultyRank(difficultyRank)
                .difficultyFeaturesJson(difficultyFeaturesJson)
                .evaluationGuideJson(evaluationGuideJson)
                .suggestedIdeasJson(suggestedIdeasJson)
                .questionType(questionType)
                .maxResponseSeconds(maxResponseSeconds)
                .minResponseSeconds(minResponseSeconds)
                .vstepPart(vstepPart)
                .active(true)
                .createdAt(createdAt)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final PracticeQuestion target = new PracticeQuestion();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder practiceTopicId(UUID practiceTopicId) {
            target.practiceTopicId = practiceTopicId;
            return this;
        }

        public Builder questionText(String questionText) {
            target.questionText = questionText;
            return this;
        }

        public Builder targetCriterionCode(String targetCriterionCode) {
            target.targetCriterionCode = targetCriterionCode;
            return this;
        }

        public Builder targetSubAttribute(String targetSubAttribute) {
            target.targetSubAttribute = targetSubAttribute;
            return this;
        }

        public Builder targetTense(String targetTense) {
            target.targetTense = targetTense;
            return this;
        }

        public Builder difficultyRank(int difficultyRank) {
            target.difficultyRank = difficultyRank;
            return this;
        }

        public Builder difficultyFeaturesJson(String difficultyFeaturesJson) {
            target.difficultyFeaturesJson = difficultyFeaturesJson;
            return this;
        }

        public Builder evaluationGuideJson(String evaluationGuideJson) {
            target.evaluationGuideJson = evaluationGuideJson;
            return this;
        }

        public Builder suggestedIdeasJson(String suggestedIdeasJson) {
            target.suggestedIdeasJson = suggestedIdeasJson;
            return this;
        }

        public Builder questionType(String questionType) {
            target.questionType = questionType;
            return this;
        }

        public Builder maxResponseSeconds(int maxResponseSeconds) {
            target.maxResponseSeconds = maxResponseSeconds;
            return this;
        }

        public Builder minResponseSeconds(int minResponseSeconds) {
            target.minResponseSeconds = minResponseSeconds;
            return this;
        }

        public Builder vstepPart(Integer vstepPart) {
            target.vstepPart = vstepPart;
            return this;
        }

        public Builder source(String source) {
            target.source = source;
            return this;
        }

        public Builder usageCount(int usageCount) {
            target.usageCount = usageCount;
            return this;
        }

        public Builder active(boolean active) {
            target.active = active;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public PracticeQuestion build() {
            return target;
        }

    }
}
