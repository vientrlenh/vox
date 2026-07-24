package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_appeal_reviewer_items", indexes = {
    @Index(columnList = "appeal_reviewer_id, appeal_item_id", name = "uq_appeal_reviewer_item", unique = true),
    @Index(columnList = "appeal_reviewer_id", name = "idx_appeal_reviewer_items_reviewer")
})
public class ExamAppealReviewerItemJpaEntity {

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

    @Column(name = "appeal_reviewer_id", nullable = false, updatable = false)
    private UUID appealReviewerId;

    @Column(name = "appeal_item_id", nullable = false, updatable = false)
    private UUID appealItemId;

    /**
     * Trỏ tới exam_item_evaluations (engine HUMAN, status UNDER_REVIEW).
     * NOT NULL: giám khảo nộp báo cáo cho toàn bộ part trong một lần, không có bản dở dang.
     */
    @Column(name = "evaluation_id", nullable = false, updatable = false)
    private UUID evaluationId;

    /** Điểm đề xuất cho part này — trung bình các tiêu chí. */
    @Column(name = "suggested_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal suggestedScore;

    /** Nhận xét của giám khảo cho riêng part này. */
    @Column(name = "note", length = 2048)
    private String note;

    protected ExamAppealReviewerItemJpaEntity() {}

    public ExamAppealReviewerItemJpaEntity(UUID id, UUID appealReviewerId, UUID appealItemId, UUID evaluationId,
            BigDecimal suggestedScore, String note) {
        this.id = id;
        this.appealReviewerId = appealReviewerId;
        this.appealItemId = appealItemId;
        this.evaluationId = evaluationId;
        this.suggestedScore = suggestedScore;
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAppealReviewerId() {
        return appealReviewerId;
    }

    public void setAppealReviewerId(UUID appealReviewerId) {
        this.appealReviewerId = appealReviewerId;
    }

    public UUID getAppealItemId() {
        return appealItemId;
    }

    public void setAppealItemId(UUID appealItemId) {
        this.appealItemId = appealItemId;
    }

    public UUID getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(UUID evaluationId) {
        this.evaluationId = evaluationId;
    }

    public BigDecimal getSuggestedScore() {
        return suggestedScore;
    }

    public void setSuggestedScore(BigDecimal suggestedScore) {
        this.suggestedScore = suggestedScore;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
