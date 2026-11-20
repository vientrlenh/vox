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
@Table(name = "exam_result_appeal_items", indexes = {
    @Index(columnList = "appeal_id, paper_item_id", name = "uq_appeal_item", unique = true),
    @Index(columnList = "appeal_id", name = "idx_exam_result_appeal_items_appeal")
})
public class ExamResultAppealItemJpaEntity {

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

    @Column(name = "appeal_id", nullable = false, updatable = false)
    private UUID appealId;

    /** Part được phúc khảo. */
    @Column(name = "paper_item_id", nullable = false, updatable = false)
    private UUID paperItemId;

    /** Câu trả lời tương ứng với part trên — giám khảo nghe lại và chấm lại. */
    @Column(name = "response_id", nullable = false, updatable = false)
    private UUID responseId;

    /** Điểm công bố cho part này — NULL cho tới khi đơn được công bố. */
    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    protected ExamResultAppealItemJpaEntity() {}

    public ExamResultAppealItemJpaEntity(UUID id, UUID appealId, UUID paperItemId, UUID responseId,
            BigDecimal finalScore) {
        this.id = id;
        this.appealId = appealId;
        this.paperItemId = paperItemId;
        this.responseId = responseId;
        this.finalScore = finalScore;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAppealId() {
        return appealId;
    }

    public void setAppealId(UUID appealId) {
        this.appealId = appealId;
    }

    public UUID getPaperItemId() {
        return paperItemId;
    }

    public void setPaperItemId(UUID paperItemId) {
        this.paperItemId = paperItemId;
    }

    public UUID getResponseId() {
        return responseId;
    }

    public void setResponseId(UUID responseId) {
        this.responseId = responseId;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }
}
