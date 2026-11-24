package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class AssessmentBlueprintSection extends BaseModel {
    private UUID blueprintVersionId;
    private int order;
    private String title;
    private String instruction;
    private Integer sectionTimeLimitSeconds;
    private BigDecimal sectionWeight;

    public AssessmentBlueprintSection() {}

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Integer getSectionTimeLimitSeconds() {
        return sectionTimeLimitSeconds;
    }

    public void setSectionTimeLimitSeconds(Integer sectionTimeLimitSeconds) {
        this.sectionTimeLimitSeconds = sectionTimeLimitSeconds;
    }

    public BigDecimal getSectionWeight() {
        return sectionWeight;
    }

    public void setSectionWeight(BigDecimal sectionWeight) {
        this.sectionWeight = sectionWeight;
    }

    public static AssessmentBlueprintSection create(
            UUID blueprintVersionId,
            int order,
            String title,
            String instruction,
            Integer sectionTimeLimitSeconds,
            BigDecimal sectionWeight) {
        return builder()
            .blueprintVersionId(blueprintVersionId)
            .order(order)
            .title(title)
            .instruction(instruction)
            .sectionTimeLimitSeconds(sectionTimeLimitSeconds)
            .sectionWeight(sectionWeight)
            .build();
    }

    /** Returns a new instance that keeps this section's id, version and creation audit values. */
    public AssessmentBlueprintSection revisedWith(
            int newOrder,
            String newTitle,
            String newInstruction,
            Integer newSectionTimeLimitSeconds,
            BigDecimal newSectionWeight) {
        return builder()
            .id(getId())
            .blueprintVersionId(blueprintVersionId)
            .order(newOrder)
            .title(newTitle)
            .instruction(newInstruction)
            .sectionTimeLimitSeconds(newSectionTimeLimitSeconds)
            .sectionWeight(newSectionWeight)
            .createdAt(getCreatedAt())
            .createdBy(getCreatedBy())
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<AssessmentBlueprintSection, Builder> {

        private Builder() {
            super(new AssessmentBlueprintSection());
        }

        @Override
        protected Builder self() {
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

        public Builder title(String title) {
            target.title = title;
            return this;
        }

        public Builder instruction(String instruction) {
            target.instruction = instruction;
            return this;
        }

        public Builder sectionTimeLimitSeconds(Integer sectionTimeLimitSeconds) {
            target.sectionTimeLimitSeconds = sectionTimeLimitSeconds;
            return this;
        }

        public Builder sectionWeight(BigDecimal sectionWeight) {
            target.sectionWeight = sectionWeight;
            return this;
        }
    }
}
