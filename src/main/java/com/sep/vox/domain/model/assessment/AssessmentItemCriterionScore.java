package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.util.UUID;

public class AssessmentItemCriterionScore {
    private UUID id;
    private UUID evaluationId;
    private UUID rubricCriterionId;
    private BigDecimal rawScore;
    private BigDecimal finalScore;
    private String rationale;
    private String matchedBandCode;

    public AssessmentItemCriterionScore() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(UUID evaluationId) {
        this.evaluationId = evaluationId;
    }

    public UUID getRubricCriterionId() {
        return rubricCriterionId;
    }

    public void setRubricCriterionId(UUID rubricCriterionId) {
        this.rubricCriterionId = rubricCriterionId;
    }

    public BigDecimal getRawScore() {
        return rawScore;
    }

    public void setRawScore(BigDecimal rawScore) {
        this.rawScore = rawScore;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getMatchedBandCode() {
        return matchedBandCode;
    }

    public void setMatchedBandCode(String matchedBandCode) {
        this.matchedBandCode = matchedBandCode;
    }

    public static AssessmentItemCriterionScore scored(UUID evaluationId, UUID rubricCriterionId, BigDecimal score,
                                                      String rationale, String matchedBandCode) {
        return builder()
                .evaluationId(evaluationId)
                .rubricCriterionId(rubricCriterionId)
                .rawScore(score)
                .finalScore(score)
                .rationale(rationale)
                .matchedBandCode(matchedBandCode)
                .build();
    }

    public static AssessmentItemCriterionScore scoredWithoutBand(UUID evaluationId, UUID rubricCriterionId,
                                                                 BigDecimal score, String rationale) {
        return scored(evaluationId, rubricCriterionId, score, rationale, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentItemCriterionScore target = new AssessmentItemCriterionScore();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder evaluationId(UUID evaluationId) {
            target.evaluationId = evaluationId;
            return this;
        }

        public Builder rubricCriterionId(UUID rubricCriterionId) {
            target.rubricCriterionId = rubricCriterionId;
            return this;
        }

        public Builder rawScore(BigDecimal rawScore) {
            target.rawScore = rawScore;
            return this;
        }

        public Builder finalScore(BigDecimal finalScore) {
            target.finalScore = finalScore;
            return this;
        }

        public Builder rationale(String rationale) {
            target.rationale = rationale;
            return this;
        }

        public Builder matchedBandCode(String matchedBandCode) {
            target.matchedBandCode = matchedBandCode;
            return this;
        }

        public AssessmentItemCriterionScore build() {
            return target;
        }

    }
}
