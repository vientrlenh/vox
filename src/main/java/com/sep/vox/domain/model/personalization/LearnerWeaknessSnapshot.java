package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class LearnerWeaknessSnapshot {

    private UUID id;
    private UUID studentId;
    private UUID frameworkCriterionId;
    private BigDecimal relEstimate;
    private BigDecimal weakness;
    private int observationCount;
    private boolean reliable;
    private OffsetDateTime computedAt;

    public LearnerWeaknessSnapshot() {
    }

    public LearnerWeaknessSnapshot(
            UUID id,
            UUID studentId,
            UUID frameworkCriterionId,
            BigDecimal relEstimate,
            BigDecimal weakness,
            int observationCount,
            boolean reliable,
            OffsetDateTime computedAt) {
        this.id = id;
        this.studentId = studentId;
        this.frameworkCriterionId = frameworkCriterionId;
        this.relEstimate = relEstimate;
        this.weakness = weakness;
        this.observationCount = observationCount;
        this.reliable = reliable;
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

    public BigDecimal getRelEstimate() {
        return relEstimate;
    }

    public void setRelEstimate(BigDecimal relEstimate) {
        this.relEstimate = relEstimate;
    }

    public BigDecimal getWeakness() {
        return weakness;
    }

    public void setWeakness(BigDecimal weakness) {
        this.weakness = weakness;
    }

    public int getObservationCount() {
        return observationCount;
    }

    public void setObservationCount(int observationCount) {
        this.observationCount = observationCount;
    }

    public boolean isReliable() {
        return reliable;
    }

    public void setReliable(boolean reliable) {
        this.reliable = reliable;
    }

    public OffsetDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(OffsetDateTime computedAt) {
        this.computedAt = computedAt;
    }
}
