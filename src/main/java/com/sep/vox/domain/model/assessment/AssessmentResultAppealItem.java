package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một phần thi được phúc khảo trong đơn. Một đơn có thể có nhiều phần.
 *
 * <p>{@code responseId} được chốt ngay lúc tạo đơn: điểm chấm lại ghi theo response,
 * nên không được để nó thay đổi giữa chừng.
 */
public class AssessmentResultAppealItem {
    private UUID id;
    private UUID appealId;
    private UUID paperItemId;
    private UUID responseId;
    /** Score published for this part: null until the appeal is published. */
    private BigDecimal finalScore;

    public AssessmentResultAppealItem() {}

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

    public static AssessmentResultAppealItem forResponse(UUID appealId, UUID paperItemId, UUID responseId) {
        return builder()
            .appealId(appealId)
            .paperItemId(paperItemId)
            .responseId(responseId)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentResultAppealItem target = new AssessmentResultAppealItem();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder appealId(UUID appealId) {
            target.appealId = appealId;
            return this;
        }

        public Builder paperItemId(UUID paperItemId) {
            target.paperItemId = paperItemId;
            return this;
        }

        public Builder responseId(UUID responseId) {
            target.responseId = responseId;
            return this;
        }

        public Builder finalScore(BigDecimal finalScore) {
            target.finalScore = finalScore;
            return this;
        }

        public AssessmentResultAppealItem build() {
            return target;
        }

    }
}
