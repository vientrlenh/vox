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
    private String gradedByModel; // name of the grading model
    private Integer sampleCount; // sample count when the grading type is an ensemble
    private UUID reviewerId; // the human grader
    private BigDecimal rawItemScore; // score before the rules are applied
    private BigDecimal itemScore; // score after the rules are applied
    private BigDecimal overallConfidence; // confidence of the AI score
    private boolean requiresHumanReview; // automatically routed to a human grader when the confidence is low
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

    public AssessmentItemEvaluation() {}

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentItemEvaluation target = new AssessmentItemEvaluation();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder responseId(UUID responseId) {
            target.responseId = responseId;
            return this;
        }

        public Builder paperItemId(UUID paperItemId) {
            target.paperItemId = paperItemId;
            return this;
        }

        public Builder engineType(AssessmentEvaluationEngineType engineType) {
            target.engineType = engineType;
            return this;
        }

        public Builder gradedByModel(String gradedByModel) {
            target.gradedByModel = gradedByModel;
            return this;
        }

        public Builder sampleCount(Integer sampleCount) {
            target.sampleCount = sampleCount;
            return this;
        }

        public Builder reviewerId(UUID reviewerId) {
            target.reviewerId = reviewerId;
            return this;
        }

        public Builder rawItemScore(BigDecimal rawItemScore) {
            target.rawItemScore = rawItemScore;
            return this;
        }

        public Builder itemScore(BigDecimal itemScore) {
            target.itemScore = itemScore;
            return this;
        }

        public Builder overallConfidence(BigDecimal overallConfidence) {
            target.overallConfidence = overallConfidence;
            return this;
        }

        public Builder requiresHumanReview(boolean requiresHumanReview) {
            target.requiresHumanReview = requiresHumanReview;
            return this;
        }

        public Builder reviewReasonCode(String reviewReasonCode) {
            target.reviewReasonCode = reviewReasonCode;
            return this;
        }

        public Builder markedInvalid(boolean markedInvalid) {
            target.markedInvalid = markedInvalid;
            return this;
        }

        public Builder requiresRetake(boolean requiresRetake) {
            target.requiresRetake = requiresRetake;
            return this;
        }

        public Builder signals(EvaluationSignals signals) {
            target.signals = signals;
            return this;
        }

        public Builder validityJson(String validityJson) {
            target.validityJson = validityJson;
            return this;
        }

        public Builder feedbackSummary(String feedbackSummary) {
            target.feedbackSummary = feedbackSummary;
            return this;
        }

        public Builder suggestionsJson(String suggestionsJson) {
            target.suggestionsJson = suggestionsJson;
            return this;
        }

        public Builder promptVersion(String promptVersion) {
            target.promptVersion = promptVersion;
            return this;
        }

        public Builder status(AssessmentItemEvaluationStatus status) {
            target.status = status;
            return this;
        }

        public Builder evaluatedAt(Instant evaluatedAt) {
            target.evaluatedAt = evaluatedAt;
            return this;
        }

        public AssessmentItemEvaluation build() {
            return target;
        }

    }
}
