package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

public class AssessmentBlueprintSlot extends BaseModel {
    private UUID sectionId;
    private UUID blueprintVersionId;
    private int order;
    private BigDecimal weight;
    private Integer prepTimeSecondsOverride; // overrides the default
    private Integer responseTimeSecondsOverride;
    private AssessmentBlueprintSlotType slotType;
    private UUID fixedQuestionId; // set when FIXED
    private QuestionSelectionSpec selectionSpec; // set when SELECTION

    public AssessmentBlueprintSlot() {}

    public UUID getSectionId() {
        return sectionId;
    }

    public void setSectionId(UUID sectionId) {
        this.sectionId = sectionId;
    }

    public UUID getBlueprintVersionId() {
        return blueprintVersionId;
    }

    public void setBlueprintVersionId(UUID blueprintVersionId) {
        this.blueprintVersionId = blueprintVersionId;
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

    public Integer getPrepTimeSecondsOverride() {
        return prepTimeSecondsOverride;
    }

    public void setPrepTimeSecondsOverride(Integer prepTimeSecondsOverride) {
        this.prepTimeSecondsOverride = prepTimeSecondsOverride;
    }

    public Integer getResponseTimeSecondsOverride() {
        return responseTimeSecondsOverride;
    }

    public void setResponseTimeSecondsOverride(Integer responseTimeSecondsOverride) {
        this.responseTimeSecondsOverride = responseTimeSecondsOverride;
    }

    public AssessmentBlueprintSlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(AssessmentBlueprintSlotType slotType) {
        this.slotType = slotType;
    }

    public UUID getFixedQuestionId() {
        return fixedQuestionId;
    }

    public void setFixedQuestionId(UUID fixedQuestionId) {
        this.fixedQuestionId = fixedQuestionId;
    }

    public QuestionSelectionSpec getSelectionSpec() {
        return selectionSpec;
    }

    public void setSelectionSpec(QuestionSelectionSpec selectionSpec) {
        this.selectionSpec = selectionSpec;
    }

    public static AssessmentBlueprintSlot create(
            UUID sectionId,
            UUID blueprintVersionId,
            int order,
            BigDecimal weight,
            Integer prepTimeSecondsOverride,
            Integer responseTimeSecondsOverride,
            AssessmentBlueprintSlotType slotType,
            UUID fixedQuestionId,
            QuestionSelectionSpec selectionSpec) {
        return builder()
            .sectionId(sectionId)
            .blueprintVersionId(blueprintVersionId)
            .order(order)
            .weight(weight)
            .prepTimeSecondsOverride(prepTimeSecondsOverride)
            .responseTimeSecondsOverride(responseTimeSecondsOverride)
            .slotType(slotType)
            .fixedQuestionId(fixedQuestionId)
            .selectionSpec(selectionSpec)
            .build();
    }

    /** Returns a new instance that keeps this slot's id, section, version and creation audit values. */
    public AssessmentBlueprintSlot revisedWith(
            int newOrder,
            BigDecimal newWeight,
            Integer newPrepTimeSecondsOverride,
            Integer newResponseTimeSecondsOverride,
            AssessmentBlueprintSlotType newSlotType,
            UUID newFixedQuestionId,
            QuestionSelectionSpec newSelectionSpec) {
        return builder()
            .id(getId())
            .sectionId(sectionId)
            .blueprintVersionId(blueprintVersionId)
            .order(newOrder)
            .weight(newWeight)
            .prepTimeSecondsOverride(newPrepTimeSecondsOverride)
            .responseTimeSecondsOverride(newResponseTimeSecondsOverride)
            .slotType(newSlotType)
            .fixedQuestionId(newFixedQuestionId)
            .selectionSpec(newSelectionSpec)
            .createdAt(getCreatedAt())
            .createdBy(getCreatedBy())
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<AssessmentBlueprintSlot, Builder> {

        private Builder() {
            super(new AssessmentBlueprintSlot());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder sectionId(UUID sectionId) {
            target.sectionId = sectionId;
            return this;
        }

        public Builder blueprintVersionId(UUID blueprintVersionId) {
            target.blueprintVersionId = blueprintVersionId;
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

        public Builder prepTimeSecondsOverride(Integer prepTimeSecondsOverride) {
            target.prepTimeSecondsOverride = prepTimeSecondsOverride;
            return this;
        }

        public Builder responseTimeSecondsOverride(Integer responseTimeSecondsOverride) {
            target.responseTimeSecondsOverride = responseTimeSecondsOverride;
            return this;
        }

        public Builder slotType(AssessmentBlueprintSlotType slotType) {
            target.slotType = slotType;
            return this;
        }

        public Builder fixedQuestionId(UUID fixedQuestionId) {
            target.fixedQuestionId = fixedQuestionId;
            return this;
        }

        public Builder selectionSpec(QuestionSelectionSpec selectionSpec) {
            target.selectionSpec = selectionSpec;
            return this;
        }
    }
}
