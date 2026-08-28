package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "practice_item_evaluations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_evaluation_response",
        columnNames = "practice_response_id"
    ),
    indexes = @Index(name = "idx_practice_evaluation_time", columnList = "evaluated_at")
)
public class PracticeItemEvaluationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "practice_response_id", nullable = false, updatable = false)
    private UUID practiceResponseId;
    @Column(name = "item_score", precision = 5, scale = 2)
    private BigDecimal itemScore;
    @Column(name = "marked_invalid", nullable = false)
    private boolean markedInvalid;
    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected PracticeItemEvaluationJpaEntity() {
    }

    public PracticeItemEvaluationJpaEntity(
            UUID id,
            UUID practiceResponseId,
            BigDecimal itemScore,
            boolean markedInvalid,
            Instant evaluatedAt) {
        this.id = id;
        this.practiceResponseId = practiceResponseId;
        this.itemScore = itemScore;
        this.markedInvalid = markedInvalid;
        this.evaluatedAt = evaluatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPracticeResponseId() {
        return practiceResponseId;
    }

    public BigDecimal getItemScore() {
        return itemScore;
    }

    public void setItemScore(BigDecimal itemScore) {
        this.itemScore = itemScore;
    }

    public boolean isMarkedInvalid() {
        return markedInvalid;
    }

    public void setMarkedInvalid(boolean markedInvalid) {
        this.markedInvalid = markedInvalid;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
