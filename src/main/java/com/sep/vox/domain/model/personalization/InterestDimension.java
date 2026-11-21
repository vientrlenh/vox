package com.sep.vox.domain.model.personalization;

import java.time.Instant;

public class InterestDimension {

    private String code;
    private String label;
    private String description;
    private boolean active;
    private boolean quizEligible;
    private int displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public InterestDimension() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isQuizEligible() {
        return quizEligible;
    }

    public void setQuizEligible(boolean quizEligible) {
        this.quizEligible = quizEligible;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final InterestDimension target = new InterestDimension();

        private Builder() {
        }

        public Builder code(String code) {
            target.code = code;
            return this;
        }

        public Builder label(String label) {
            target.label = label;
            return this;
        }

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder active(boolean active) {
            target.active = active;
            return this;
        }

        public Builder quizEligible(boolean quizEligible) {
            target.quizEligible = quizEligible;
            return this;
        }

        public Builder displayOrder(int displayOrder) {
            target.displayOrder = displayOrder;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            target.updatedAt = updatedAt;
            return this;
        }

        public InterestDimension build() {
            return target;
        }

    }
}
