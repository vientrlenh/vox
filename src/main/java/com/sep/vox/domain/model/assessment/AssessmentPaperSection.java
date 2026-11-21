package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class AssessmentPaperSection extends BaseModel {
    private UUID paperId;
    private int order;
    private String title;
    private String instruction;
    private Integer sectionTimeLimitSeconds;
    private BigDecimal weight;

    public AssessmentPaperSection() {}

    public UUID getPaperId() {
        return paperId;
    }

    public void setPaperId(UUID paperId) {
        this.paperId = paperId;
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

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<AssessmentPaperSection, Builder> {

        private Builder() {
            super(new AssessmentPaperSection());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder paperId(UUID paperId) {
            target.paperId = paperId;
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

        public Builder weight(BigDecimal weight) {
            target.weight = weight;
            return this;
        }
    }
}
