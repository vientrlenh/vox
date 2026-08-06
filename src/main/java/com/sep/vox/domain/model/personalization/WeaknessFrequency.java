package com.sep.vox.domain.model.personalization;

import java.util.UUID;

public class WeaknessFrequency {

    private UUID studentId;
    private UUID frameworkCriterionId;
    private String subAttribute;
    private int frequency;
    private double decayedFrequency;

    public WeaknessFrequency() {
    }

    public WeaknessFrequency(
            UUID studentId,
            UUID frameworkCriterionId,
            String subAttribute,
            int frequency,
            double decayedFrequency) {
        this.studentId = studentId;
        this.frameworkCriterionId = frameworkCriterionId;
        this.subAttribute = subAttribute;
        this.frequency = frequency;
        this.decayedFrequency = decayedFrequency;
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

    public double getDecayedFrequency() {
        return decayedFrequency;
    }

    public void setDecayedFrequency(double decayedFrequency) {
        this.decayedFrequency = decayedFrequency;
    }
}
