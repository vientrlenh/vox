package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "practice_criterion_score",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_criterion_score_evaluation_criterion",
        columnNames = {"practice_evaluation_id", "rubric_criterion_id"}
    ),
    indexes = @Index(name = "idx_practice_criterion_score_evaluation", columnList = "practice_evaluation_id")
)
public class PracticeCriterionScoreJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "practice_evaluation_id", nullable = false, updatable = false)
    private UUID practiceEvaluationId;
    @Column(name = "rubric_criterion_id", nullable = false, updatable = false)
    private UUID rubricCriterionId;
    @Column(name = "final_score", precision = 7, scale = 3)
    private BigDecimal finalScore;
    @Column(name = "matched_band_code", length = 64)
    private String matchedBandCode;

    protected PracticeCriterionScoreJpaEntity() {
    }

    public PracticeCriterionScoreJpaEntity(
            UUID id,
            UUID practiceEvaluationId,
            UUID rubricCriterionId,
            BigDecimal finalScore,
            String matchedBandCode) {
        this.id = id;
        this.practiceEvaluationId = practiceEvaluationId;
        this.rubricCriterionId = rubricCriterionId;
        this.finalScore = finalScore;
        this.matchedBandCode = matchedBandCode;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPracticeEvaluationId() {
        return practiceEvaluationId;
    }

    public UUID getRubricCriterionId() {
        return rubricCriterionId;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public String getMatchedBandCode() {
        return matchedBandCode;
    }

    public void setMatchedBandCode(String matchedBandCode) {
        this.matchedBandCode = matchedBandCode;
    }
}
