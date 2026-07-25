package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Báo cáo chấm lại của một giám khảo cho một phần thi trong đơn phúc khảo.
 *
 * <p>Giám khảo nộp báo cáo cho toàn bộ phần thi trong một lần, nên mỗi dòng ở đây
 * luôn có đủ {@code evaluationId} và {@code suggestedScore} — không có báo cáo dở dang.
 */
public class ExamAppealReviewerItem {
    private UUID id;
    private UUID appealReviewerId;
    private UUID appealItemId;
    private UUID evaluationId;
    private BigDecimal suggestedScore;
    private String note;

    public ExamAppealReviewerItem() {}

    public ExamAppealReviewerItem(UUID id, UUID appealReviewerId, UUID appealItemId, UUID evaluationId,
            BigDecimal suggestedScore, String note) {
        this.id = id;
        this.appealReviewerId = appealReviewerId;
        this.appealItemId = appealItemId;
        this.evaluationId = evaluationId;
        this.suggestedScore = suggestedScore;
        this.note = note;
    }

    public ExamAppealReviewerItem(UUID appealReviewerId, UUID appealItemId, UUID evaluationId,
            BigDecimal suggestedScore, String note) {
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
