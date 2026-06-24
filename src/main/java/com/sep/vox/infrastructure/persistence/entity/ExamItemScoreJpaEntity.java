package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_item_scores")
public class ExamItemScoreJpaEntity {
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

    @Column(name = "response_id", nullable = false, updatable = false)
    private UUID responseId;

    @Column(name = "paper_item_id", nullable = false, updatable = false)
    private UUID paperItemId;

    @Column(name = "rubric_scores", columnDefinition = "TEXT")
    private String rubricScores;

    @Column(name = "item_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal itemScore;

    @Column(name = "graded_by_model", nullable = false, updatable = false, length = 100)
    private String gradedByModel;

    @Column(name = "graded_at", nullable = false, updatable = false)
    private OffsetDateTime gradedAt;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_item_scores_status_valid", 
            constraint = "status IN ('AUTO_GRADED', 'UNDER_REVIEW', 'FINALIZED')"
        )
    }
    )
    private String status;

    protected ExamItemScoreJpaEntity() {}

    public ExamItemScoreJpaEntity(UUID id, UUID responseId, UUID paperItemId, String rubricScores, BigDecimal itemScore,
            String gradedByModel, OffsetDateTime gradedAt, String status) {
        this.id = id;
        this.responseId = responseId;
        this.paperItemId = paperItemId;
        this.rubricScores = rubricScores;
        this.itemScore = itemScore;
        this.gradedByModel = gradedByModel;
        this.gradedAt = gradedAt;
        this.status = status;
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

    public String getRubricScores() {
        return rubricScores;
    }

    public void setRubricScores(String rubricScores) {
        this.rubricScores = rubricScores;
    }

    public BigDecimal getItemScore() {
        return itemScore;
    }

    public void setItemScore(BigDecimal itemScore) {
        this.itemScore = itemScore;
    }

    public String getGradedByModel() {
        return gradedByModel;
    }

    public void setGradedByModel(String gradedByModel) {
        this.gradedByModel = gradedByModel;
    }

    public OffsetDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(OffsetDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    
}
