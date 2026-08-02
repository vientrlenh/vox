package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.util.UUID;

public class ExamItemCriterionScore {
    private UUID id;
    private UUID evaluationId;
    private UUID rubricCriterionId;
    private BigDecimal rawScore;
    private BigDecimal finalScore;
    private String rationale;
    private String matchedBandCode;

    public ExamItemCriterionScore() {}

    public ExamItemCriterionScore(UUID id, UUID evaluationId, UUID rubricCriterionId, BigDecimal rawScore,
            BigDecimal finalScore, String rationale) {
        this(id, evaluationId, rubricCriterionId, rawScore, finalScore, rationale, null);
    }

    public ExamItemCriterionScore(UUID id, UUID evaluationId, UUID rubricCriterionId, BigDecimal rawScore,
            BigDecimal finalScore, String rationale, String matchedBandCode) {
        this.id = id;
        this.evaluationId = evaluationId;
        this.rubricCriterionId = rubricCriterionId;
        this.rawScore = rawScore;
        this.finalScore = finalScore;
        this.rationale = rationale;
        this.matchedBandCode = matchedBandCode;
    }

    public ExamItemCriterionScore(UUID evaluationId, UUID rubricCriterionId, BigDecimal rawScore, BigDecimal finalScore,
            String rationale) {
        this(evaluationId, rubricCriterionId, rawScore, finalScore, rationale, null);
    }

    public ExamItemCriterionScore(UUID evaluationId, UUID rubricCriterionId, BigDecimal rawScore, BigDecimal finalScore,
            String rationale, String matchedBandCode) {
        this.evaluationId = evaluationId;
        this.rubricCriterionId = rubricCriterionId;
        this.rawScore = rawScore;
        this.finalScore = finalScore;
        this.rationale = rationale;
        this.matchedBandCode = matchedBandCode;
    }

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
}
