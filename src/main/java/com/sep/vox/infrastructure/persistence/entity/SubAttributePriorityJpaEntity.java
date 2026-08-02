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
    name = "sub_attribute_priority",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_sub_attribute_priority_student_criterion_attribute",
        columnNames = {"student_id", "framework_criterion_id", "sub_attribute"}
    ),
    indexes = @Index(
        name = "idx_sub_attribute_priority_student",
        columnList = "student_id"
    )
)
public class SubAttributePriorityJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "framework_criterion_id", nullable = false)
    private UUID frameworkCriterionId;

    @Column(name = "sub_attribute", nullable = false, length = 64)
    private String subAttribute;

    @Column(name = "freq", nullable = false)
    private int frequency;

    @Column(name = "recent_freq", nullable = false)
    private int recentFrequency;

    @Column(name = "priority", nullable = false, precision = 6, scale = 4)
    private BigDecimal priority;

    @Column(name = "practiceable", nullable = false)
    private boolean practiceable;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    protected SubAttributePriorityJpaEntity() {
    }

    public SubAttributePriorityJpaEntity(
            UUID id,
            UUID studentId,
            UUID frameworkCriterionId,
            String subAttribute,
            int frequency,
            int recentFrequency,
            BigDecimal priority,
            boolean practiceable,
            OffsetDateTime computedAt) {
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

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getFrameworkCriterionId() {
        return frameworkCriterionId;
    }

    public String getSubAttribute() {
        return subAttribute;
    }

    public int getFrequency() {
        return frequency;
    }

    public int getRecentFrequency() {
        return recentFrequency;
    }

    public BigDecimal getPriority() {
        return priority;
    }

    public boolean isPracticeable() {
        return practiceable;
    }

    public OffsetDateTime getComputedAt() {
        return computedAt;
    }
}
