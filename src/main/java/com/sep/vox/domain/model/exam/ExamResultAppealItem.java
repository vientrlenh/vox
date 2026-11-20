package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một phần thi được phúc khảo trong đơn. Một đơn có thể có nhiều phần.
 *
 * <p>{@code responseId} được chốt ngay lúc tạo đơn: điểm chấm lại ghi theo response,
 * nên không được để nó thay đổi giữa chừng.
 */
public class ExamResultAppealItem {
    private UUID id;
    private UUID appealId;
    private UUID paperItemId;
    private UUID responseId;
    /** Điểm công bố cho phần này — null cho tới khi đơn được công bố. */
    private BigDecimal finalScore;

    public ExamResultAppealItem() {}

    public ExamResultAppealItem(UUID id, UUID appealId, UUID paperItemId, UUID responseId,
            BigDecimal finalScore) {
        this.id = id;
        this.appealId = appealId;
        this.paperItemId = paperItemId;
        this.responseId = responseId;
        this.finalScore = finalScore;
    }

    public ExamResultAppealItem(UUID appealId, UUID paperItemId, UUID responseId, BigDecimal finalScore) {
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
