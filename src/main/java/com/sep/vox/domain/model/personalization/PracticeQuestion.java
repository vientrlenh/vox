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
     * Thì mà câu này ép học sinh trả lời -- PRESENT/PAST/FUTURE/PERFECT/CONDITIONAL, xem
     * {@code TensePolicy}. {@code null} nghĩa là CHƯA BIẾT (câu soạn trước khi có cột này),
     * không phải "không ép thì nào"; thang leo chọn câu coi null là dùng được.
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

    public PracticeQuestion(
            UUID id,
            UUID practiceTopicId,
            String questionText,
            String targetCriterionCode,
            String targetSubAttribute,
            String targetTense,
            int difficultyRank,
            String difficultyFeaturesJson,
            String evaluationGuideJson,
            String suggestedIdeasJson,
            String questionType,
            int maxResponseSeconds,
            int minResponseSeconds,
            Integer vstepPart,
            String source,
            int usageCount,
            boolean active,
            Instant createdAt) {
        this.id = id;
        this.practiceTopicId = practiceTopicId;
        this.questionText = questionText;
        this.targetCriterionCode = targetCriterionCode;
        this.targetSubAttribute = targetSubAttribute;
        this.targetTense = targetTense;
        this.difficultyRank = difficultyRank;
        this.difficultyFeaturesJson = difficultyFeaturesJson;
        this.evaluationGuideJson = evaluationGuideJson;
        this.suggestedIdeasJson = suggestedIdeasJson;
        this.questionType = questionType;
        this.maxResponseSeconds = maxResponseSeconds;
        this.minResponseSeconds = minResponseSeconds;
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

    /** SHORT_ANSWER | LONG_ANSWER | DESCRIPTION | OPINION -- xem migration V13. */
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
     * Trần nói của CẢ câu, gồm cả chuỗi follow-up.
     *
     * Trước đây là {@code maxResponseSeconds + maxFollowupSeconds}, nhưng cột follow-up là cột
     * chết (LLM tự điền mà prompt không hề mô tả, nên toàn kho bằng 0) -- tổng đó luôn bằng
     * đúng maxResponseSeconds. Giờ nói thẳng ra thay vì cộng một số hạng luôn bằng 0.
     *
     * Follow-up không có ngân sách riêng vì SignalNode vốn đã cộng dồn giây qua mọi lượt của
     * cùng một câu; cái nó thiếu là SÀN để biết khi nào đủ, và đó là {@link #getMinResponseSeconds()}.
     */
    public int spokenSeconds() {
        return maxResponseSeconds;
    }
}
