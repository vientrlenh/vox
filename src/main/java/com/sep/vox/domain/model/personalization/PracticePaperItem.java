package com.sep.vox.domain.model.personalization;

import java.util.UUID;

public class PracticePaperItem {

    private UUID id;
    private UUID practicePaperId;
    private UUID practiceQuestionId;
    private int slotOrder;
    private String targetCriterionCode;
    private String targetSubAttribute;
    private int targetDifficultyRank;

    public PracticePaperItem() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPracticePaperId() {
        return practicePaperId;
    }

    public void setPracticePaperId(UUID practicePaperId) {
        this.practicePaperId = practicePaperId;
    }

    public UUID getPracticeQuestionId() {
        return practiceQuestionId;
    }

    public void setPracticeQuestionId(UUID practiceQuestionId) {
        this.practiceQuestionId = practiceQuestionId;
    }

    public int getSlotOrder() {
        return slotOrder;
    }

    public void setSlotOrder(int slotOrder) {
        this.slotOrder = slotOrder;
    }

    public String getTargetCriterionCode() {
        return targetCriterionCode;
    }

    public void setTargetCriterionCode(String targetCriterionCode) {
        this.targetCriterionCode = targetCriterionCode;
    }

    public String getTargetSubAttribute() {
        return targetSubAttribute;
    }

    public void setTargetSubAttribute(String targetSubAttribute) {
        this.targetSubAttribute = targetSubAttribute;
    }

    public int getTargetDifficultyRank() {
        return targetDifficultyRank;
    }

    public void setTargetDifficultyRank(int targetDifficultyRank) {
        this.targetDifficultyRank = targetDifficultyRank;
    }

    public static PracticePaperItem forSlot(UUID practicePaperId, UUID practiceQuestionId, int slotOrder,
                                            String targetCriterionCode, String targetSubAttribute,
                                            int targetDifficultyRank) {
        return builder()
                .practicePaperId(practicePaperId)
                .practiceQuestionId(practiceQuestionId)
                .slotOrder(slotOrder)
                .targetCriterionCode(targetCriterionCode)
                .targetSubAttribute(targetSubAttribute)
                .targetDifficultyRank(targetDifficultyRank)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final PracticePaperItem target = new PracticePaperItem();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder practicePaperId(UUID practicePaperId) {
            target.practicePaperId = practicePaperId;
            return this;
        }

        public Builder practiceQuestionId(UUID practiceQuestionId) {
            target.practiceQuestionId = practiceQuestionId;
            return this;
        }

        public Builder slotOrder(int slotOrder) {
            target.slotOrder = slotOrder;
            return this;
        }

        public Builder targetCriterionCode(String targetCriterionCode) {
            target.targetCriterionCode = targetCriterionCode;
            return this;
        }

        public Builder targetSubAttribute(String targetSubAttribute) {
            target.targetSubAttribute = targetSubAttribute;
            return this;
        }

        public Builder targetDifficultyRank(int targetDifficultyRank) {
            target.targetDifficultyRank = targetDifficultyRank;
            return this;
        }

        public PracticePaperItem build() {
            return target;
        }

    }
}
