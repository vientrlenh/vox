package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_item_criterion_scores")
public class ExamItemCriterionScoreJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "evaluation_id", nullable = false, updatable = false)
    private UUID evaluationId;

    @Column(name = "rubric_criterion_id", nullable = false, updatable = false)
    private UUID rubricCriterionId;

    @Column(name = "raw_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal rawScore;

    @Column(name = "final_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "rationale", length = 512, updatable = false)
    private String rationale; 

    protected ExamItemCriterionScoreJpaEntity() {}

    public ExamItemCriterionScoreJpaEntity(UUID id, UUID evaluationId, UUID rubricCriterionId, BigDecimal rawScore,
            BigDecimal finalScore, String rationale) {
        this.id = id;
        this.evaluationId = evaluationId;
        this.rubricCriterionId = rubricCriterionId;
        this.rawScore = rawScore;
        this.finalScore = finalScore;
        this.rationale = rationale;
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

    
}
