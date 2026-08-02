package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "learner_weakness_snapshot",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_learner_weakness_snapshot_student_criterion",
        columnNames = {"student_id", "framework_criterion_id"}
    ),
    indexes = @Index(
        name = "idx_learner_weakness_snapshot_student",
        columnList = "student_id"
    )
)
public class LearnerWeaknessSnapshotJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "framework_criterion_id", nullable = false)
    private UUID frameworkCriterionId;

    @Column(name = "rel_estimate", nullable = false, precision = 6, scale = 4)
    private BigDecimal relEstimate;

    @Column(name = "weakness", nullable = false, precision = 6, scale = 4)
    private BigDecimal weakness;

    @Column(name = "observation_count", nullable = false)
    private int observationCount;

    @Column(name = "reliable", nullable = false)
    private boolean reliable;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    protected LearnerWeaknessSnapshotJpaEntity() {
    }

    public LearnerWeaknessSnapshotJpaEntity(
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

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getFrameworkCriterionId() {
        return frameworkCriterionId;
    }

    public BigDecimal getRelEstimate() {
        return relEstimate;
    }

    public BigDecimal getWeakness() {
        return weakness;
    }

    public int getObservationCount() {
        return observationCount;
    }

    public boolean isReliable() {
        return reliable;
    }

    public OffsetDateTime getComputedAt() {
        return computedAt;
    }
}
