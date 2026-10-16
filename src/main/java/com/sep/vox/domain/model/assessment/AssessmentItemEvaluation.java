package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.valueobject.EvaluationSignals;

public class AssessmentItemEvaluation {
    private UUID id;
    private UUID responseId;
    private UUID paperItemId;
    private AssessmentEvaluationEngineType engineType;
    private String gradedByModel; // tên model chấm
    private Integer sampleCount; // số mẫu nếu loại chấm là ensemble
    private UUID reviewerId; // người chấm (human)
    private BigDecimal rawItemScore; // điểm trước khi áp dụng rule
    private BigDecimal itemScore; // điểm sau khi áp dụng rule
    private BigDecimal overallConfidence; // độ tự tin của điểm AI
    private boolean requiresHumanReview; // tự đẩy cho người chấm nếu độ tự tin thấp
    private String reviewReasonCode;
    private boolean markedInvalid;
    private boolean requiresRetake;
    private EvaluationSignals signals;
    private String validityJson;
    private String feedbackSummary;
    private String suggestionsJson;
    private String promptVersion;
    private AssessmentItemEvaluationStatus status; 
    private Instant evaluatedAt;

    protected AssessmentItemEvaluation() {}

    public AssessmentItemEvaluation(UUID id, UUID responseId, UUID paperItemId, AssessmentEvaluationEngineType engineType,
            String gradedByModel, Integer sampleCount, UUID reviewerId, BigDecimal rawItemScore, BigDecimal itemScore,
            BigDecimal overallConfidence, boolean requiresHumanReview, String reviewReasonCode, boolean markedInvalid,
            boolean requiresRetake, EvaluationSignals signals, String validityJson, String feedbackSummary, String suggestionsJson,
            String promptVersion, AssessmentItemEvaluationStatus status, Instant evaluatedAt) {
        this.id = id;
        this.responseId = responseId;
        this.paperItemId = paperItemId;
        this.engineType = engineType;
        this.gradedByModel = gradedByModel;
        this.sampleCount = sampleCount;
        this.reviewerId = reviewerId;
        this.rawItemScore = rawItemScore;
        this.itemScore = itemScore;
        this.overallConfidence = overallConfidence;
        this.requiresHumanReview = requiresHumanReview;
        this.reviewReasonCode = reviewReasonCode;
        this.markedInvalid = markedInvalid;
        this.requiresRetake = requiresRetake;
        this.signals = signals;
        this.validityJson = validityJson;
        this.feedbackSummary = feedbackSummary;
        this.suggestionsJson = suggestionsJson;
        this.promptVersion = promptVersion;
        this.status = status;
        this.evaluatedAt = evaluatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getResponseId() {
        return responseId;
    }

    public void setResponseId(UUID responseId) {
        this.responseId = responseId;
    }

    public UUID getPaperItemId() {
        return paperItemId;
    }

    public void setPaperItemId(UUID paperItemId) {
        this.paperItemId = paperItemId;
    }

    public AssessmentEvaluationEngineType getEngineType() {
        return engineType;
    }

    public void setEngineType(AssessmentEvaluationEngineType engineType) {
        this.engineType = engineType;
    }

    public String getGradedByModel() {
        return gradedByModel;
    }

    public void setGradedByModel(String gradedByModel) {
        this.gradedByModel = gradedByModel;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    public BigDecimal getRawItemScore() {
        return rawItemScore;
    }

    public void setRawItemScore(BigDecimal rawItemScore) {
        this.rawItemScore = rawItemScore;
    }

    public BigDecimal getItemScore() {
        return itemScore;
    }

    public void setItemScore(BigDecimal itemScore) {
        this.itemScore = itemScore;
    }

    public BigDecimal getOverallConfidence() {
        return overallConfidence;
    }

    public void setOverallConfidence(BigDecimal overallConfidence) {
        this.overallConfidence = overallConfidence;
    }

    public boolean isRequiresHumanReview() {
        return requiresHumanReview;
    }

    public void setRequiresHumanReview(boolean requiresHumanReview) {
        this.requiresHumanReview = requiresHumanReview;
    }

    public String getReviewReasonCode() {
        return reviewReasonCode;
    }

    public void setReviewReasonCode(String reviewReasonCode) {
        this.reviewReasonCode = reviewReasonCode;
    }

    public boolean isMarkedInvalid() {
        return markedInvalid;
    }

    public void setMarkedInvalid(boolean markedInvalid) {
        this.markedInvalid = markedInvalid;
    }

    public boolean isRequiresRetake() {
        return requiresRetake;
    }

    public void setRequiresRetake(boolean requiresRetake) {
        this.requiresRetake = requiresRetake;
    }

    public EvaluationSignals getSignals() {
        return signals;
    }

    public void setSignals(EvaluationSignals signals) {
        this.signals = signals;
    }

    public String getValidityJson() {
        return validityJson;
    }

    public void setValidityJson(String validityJson) {
        this.validityJson = validityJson;
    }

    public AssessmentItemEvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentItemEvaluationStatus status) {
        this.status = status;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public String getFeedbackSummary() {
        return feedbackSummary;
    }

    public void setFeedbackSummary(String feedbackSummary) {
        this.feedbackSummary = feedbackSummary;
    }

    public String getSuggestionsJson() {
        return suggestionsJson;
    }

    public void setSuggestionsJson(String suggestionsJson) {
        this.suggestionsJson = suggestionsJson;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }
}
