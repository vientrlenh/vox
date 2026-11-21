package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.util.UUID;

public class AssessmentPaperItem {
    private UUID id;
    private UUID blueprintSlotId;
    private UUID sectionId;
    private UUID paperId;
    private UUID questionId;
    private int order;
    private BigDecimal weight;

    public AssessmentPaperItem() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBlueprintSlotId() {
        return blueprintSlotId;
    }

    public void setBlueprintSlotId(UUID blueprintSlotId) {
        this.blueprintSlotId = blueprintSlotId;
    }

    public UUID getSectionId() {
        return sectionId;
    }

    public void setSectionId(UUID sectionId) {
        this.sectionId = sectionId;
    }

    public UUID getPaperId() {
        return paperId;
    }

    public void setPaperId(UUID paperId) {
        this.paperId = paperId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentPaperItem target = new AssessmentPaperItem();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder blueprintSlotId(UUID blueprintSlotId) {
            target.blueprintSlotId = blueprintSlotId;
            return this;
        }

        public Builder sectionId(UUID sectionId) {
            target.sectionId = sectionId;
            return this;
        }

        public Builder paperId(UUID paperId) {
            target.paperId = paperId;
            return this;
        }

        public Builder questionId(UUID questionId) {
            target.questionId = questionId;
            return this;
        }

        public Builder order(int order) {
            target.order = order;
            return this;
        }

        public Builder weight(BigDecimal weight) {
            target.weight = weight;
            return this;
        }

        public AssessmentPaperItem build() {
            return target;
        }
    }
}
