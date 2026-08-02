package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;

public class InterestDimension {

    private String code;
    private String label;
    private String description;
    private boolean active;
    private boolean quizEligible;
    private int displayOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public InterestDimension() {
    }

    public InterestDimension(
            String code,
            String label,
            String description,
            boolean active,
            boolean quizEligible,
            int displayOrder,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
