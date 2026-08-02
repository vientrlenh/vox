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

    public PracticePaperItem(
            UUID id,
            UUID practicePaperId,
            UUID practiceQuestionId,
            int slotOrder,
            String targetCriterionCode,
            String targetSubAttribute,
            int targetDifficultyRank) {
        this.id = id;
        this.practicePaperId = practicePaperId;
        this.practiceQuestionId = practiceQuestionId;
        this.slotOrder = slotOrder;
        this.targetCriterionCode = targetCriterionCode;
        this.targetSubAttribute = targetSubAttribute;
        this.targetDifficultyRank = targetDifficultyRank;
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
}
