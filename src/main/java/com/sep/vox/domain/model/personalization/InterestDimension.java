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

    public InterestDimension(
            String code,
            String label,
            String description,
            boolean active,
            boolean quizEligible,
            int displayOrder,
            Instant createdAt,
            Instant updatedAt) {
        this.code = code;
        this.label = label;
        this.description = description;
        this.active = active;
        this.quizEligible = quizEligible;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
}
