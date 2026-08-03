package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SubAttributePriority {

    private UUID id;
    private UUID studentId;
    private UUID frameworkCriterionId;
    private String subAttribute;
    private int frequency;
    private int recentFrequency;
    private BigDecimal priority;
    private boolean practiceable;
    private Instant computedAt;

    public SubAttributePriority() {
    }

    public SubAttributePriority(
            UUID id,
            UUID studentId,
            UUID frameworkCriterionId,
            String subAttribute,
            int frequency,
            int recentFrequency,
            BigDecimal priority,
            boolean practiceable,
            Instant computedAt) {
        this.id = id;
        this.studentId = studentId;
        this.frameworkCriterionId = frameworkCriterionId;
        this.subAttribute = subAttribute;
        this.frequency = frequency;
        this.recentFrequency = recentFrequency;
        this.priority = priority;
        this.practiceable = practiceable;
        this.computedAt = computedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public UUID getFrameworkCriterionId() {
        return frameworkCriterionId;
    }

    public void setFrameworkCriterionId(UUID frameworkCriterionId) {
        this.frameworkCriterionId = frameworkCriterionId;
    }

    public String getSubAttribute() {
        return subAttribute;
    }

    public void setSubAttribute(String subAttribute) {
        this.subAttribute = subAttribute;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getRecentFrequency() {
        return recentFrequency;
    }

    public void setRecentFrequency(int recentFrequency) {
        this.recentFrequency = recentFrequency;
    }

    public BigDecimal getPriority() {
        return priority;
    }

    public void setPriority(BigDecimal priority) {
        this.priority = priority;
    }

    public boolean isPracticeable() {
        return practiceable;
    }

    public void setPracticeable(boolean practiceable) {
        this.practiceable = practiceable;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }
}
