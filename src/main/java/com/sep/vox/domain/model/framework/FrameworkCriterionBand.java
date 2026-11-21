package com.sep.vox.domain.model.framework;

import com.sep.vox.domain.model.BaseModel;
import java.util.UUID;

import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public class FrameworkCriterionBand extends BaseModel {
    private UUID frameworkCriterionId;
    private UUID frameworkResultBandId;
    private String descriptor;
    private FrameworkCriterionSignals positiveSignals;
    private FrameworkCriterionSignals negativeSignals;

    public FrameworkCriterionBand() {}

    public UUID getFrameworkCriterionId() {
        return frameworkCriterionId;
    }

    public void setFrameworkCriterionId(UUID frameworkCriterionId) {
        this.frameworkCriterionId = frameworkCriterionId;
    }

    public UUID getFrameworkResultBandId() {
        return frameworkResultBandId;
    }

    public void setFrameworkResultBandId(UUID frameworkResultBandId) {
        this.frameworkResultBandId = frameworkResultBandId;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public FrameworkCriterionSignals getPositiveSignals() {
        return positiveSignals;
    }

    public void setPositiveSignals(FrameworkCriterionSignals positiveSignals) {
        this.positiveSignals = positiveSignals;
    }

    public FrameworkCriterionSignals getNegativeSignals() {
        return negativeSignals;
    }

    public void setNegativeSignals(FrameworkCriterionSignals negativeSignals) {
        this.negativeSignals = negativeSignals;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<FrameworkCriterionBand, Builder> {

        private Builder() {
            super(new FrameworkCriterionBand());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder frameworkCriterionId(UUID frameworkCriterionId) {
            target.frameworkCriterionId = frameworkCriterionId;
            return this;
        }

        public Builder frameworkResultBandId(UUID frameworkResultBandId) {
            target.frameworkResultBandId = frameworkResultBandId;
            return this;
        }

        public Builder descriptor(String descriptor) {
            target.descriptor = descriptor;
            return this;
        }

        public Builder positiveSignals(FrameworkCriterionSignals positiveSignals) {
            target.positiveSignals = positiveSignals;
            return this;
        }

        public Builder negativeSignals(FrameworkCriterionSignals negativeSignals) {
            target.negativeSignals = negativeSignals;
            return this;
        }
    }
}
