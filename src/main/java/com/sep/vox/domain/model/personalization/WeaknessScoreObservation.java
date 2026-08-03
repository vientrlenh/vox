package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class WeaknessScoreObservation {

    private UUID studentId;
    private UUID frameworkCriterionId;
    private String criterionCode;
    private BigDecimal finalScore;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private Instant evaluatedAt;
    private String sourceType;
    private UUID evaluationId;
    private UUID schoolClassId;
    private UUID schoolGradeId;

    public WeaknessScoreObservation() {
    }

    public WeaknessScoreObservation(
            UUID studentId,
            UUID frameworkCriterionId,
            String criterionCode,
            BigDecimal finalScore,
            BigDecimal minScore,
            BigDecimal maxScore,
            Instant evaluatedAt,
            String sourceType,
            UUID evaluationId,
            UUID schoolClassId,
            UUID schoolGradeId) {
        this.studentId = studentId;
        this.frameworkCriterionId = frameworkCriterionId;
        this.criterionCode = criterionCode;
        this.finalScore = finalScore;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.evaluatedAt = evaluatedAt;
        this.sourceType = sourceType;
        this.evaluationId = evaluationId;
        this.schoolClassId = schoolClassId;
        this.schoolGradeId = schoolGradeId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public UUID getFrameworkCriterionId() {
        return frameworkCriterionId;
    }

    public void setFrameworkCriterionId(UUID frameworkCriterionId) {
        this.frameworkCriterionId = frameworkCriterionId;
    }

    public String getCriterionCode() {
        return criterionCode;
    }

    public void setCriterionCode(String criterionCode) {
        this.criterionCode = criterionCode;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public void setMinScore(BigDecimal minScore) {
        this.minScore = minScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(UUID evaluationId) {
        this.evaluationId = evaluationId;
    }

    public UUID getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(UUID schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

    public UUID getSchoolGradeId() {
        return schoolGradeId;
    }

    public void setSchoolGradeId(UUID schoolGradeId) {
        this.schoolGradeId = schoolGradeId;
    }
}
