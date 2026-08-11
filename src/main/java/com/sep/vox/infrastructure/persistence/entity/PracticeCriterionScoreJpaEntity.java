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
        name = "uq_practice_criterion_score_evaluation_code",
        columnNames = {"practice_evaluation_id", "criterion_code"}
    ),
    indexes = @Index(name = "idx_practice_criterion_score_evaluation", columnList = "practice_evaluation_id")
)
public class PracticeCriterionScoreJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "practice_evaluation_id", nullable = false, updatable = false)
    private UUID practiceEvaluationId;
    /** Mã tiêu chí viết hoa (GRAMMAR, PRONUNCIATION...) -- khoá định danh thật, thay cho id rubric. */
    @Column(name = "criterion_code", nullable = false, length = 32, updatable = false)
    private String criterionCode;
    @Column(name = "final_score", precision = 7, scale = 3)
    private BigDecimal finalScore;
    protected PracticeCriterionScoreJpaEntity() {
    }

    public PracticeCriterionScoreJpaEntity(
            UUID id,
            UUID practiceEvaluationId,
            String criterionCode,
            BigDecimal finalScore) {
        this.id = id;
        this.practiceEvaluationId = practiceEvaluationId;
        this.criterionCode = criterionCode;
        this.finalScore = finalScore;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPracticeEvaluationId() {
        return practiceEvaluationId;
    }

    public String getCriterionCode() {
        return criterionCode;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

}
