package com.sep.vox.domain.model.framework;

import java.util.UUID;

import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public class FrameworkCriterionBand {
    private UUID id;
    private UUID frameworkCriterionId;
    private UUID frameworkResultBandId;
    private String descriptor;
    private FrameworkCriterionSignals positiveSignals;
    private FrameworkCriterionSignals negativeSignals;

    protected FrameworkCriterionBand() {}

    public FrameworkCriterionBand(UUID id, UUID frameworkCriterionId, UUID frameworkResultBandId, String descriptor,
            FrameworkCriterionSignals positiveSignals, FrameworkCriterionSignals negativeSignals) {
        this.id = id;
        this.frameworkCriterionId = frameworkCriterionId;
        this.frameworkResultBandId = frameworkResultBandId;
        this.descriptor = descriptor;
        this.positiveSignals = positiveSignals;
        this.negativeSignals = negativeSignals;
    }


    public FrameworkCriterionBand(Builder builder) {
        this.id = builder.id;
        this.frameworkCriterionId = builder.frameworkCriterionId;
        this.frameworkResultBandId = builder.frameworkResultBandId;
        this.descriptor = builder.descriptor;
        this.positiveSignals = builder.positiveSignals;
        this.negativeSignals = builder.negativeSignals;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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
        return new FrameworkCriterionBand.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID frameworkCriterionId;
        private UUID frameworkResultBandId;
        private String descriptor;
        private FrameworkCriterionSignals positiveSignals;
        private FrameworkCriterionSignals negativeSignals;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder frameworkCriterionId(UUID frameworkCriterionId) {
            this.frameworkCriterionId = frameworkCriterionId;
            return this;
        }

        public Builder frameworkResultBandId(UUID frameworkResultBandId) {
            this.frameworkResultBandId = frameworkResultBandId;
            return this;
        }

        public Builder descriptor(String descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        public Builder positiveSignals(FrameworkCriterionSignals positiveSignals) {
            this.positiveSignals = positiveSignals;
            return this;
        }

        public Builder negativeSignals(FrameworkCriterionSignals negativeSignals) {
            this.negativeSignals = negativeSignals;
            return this;
        }

        public FrameworkCriterionBand build() {
            return new FrameworkCriterionBand(this);
        }
    }
}
