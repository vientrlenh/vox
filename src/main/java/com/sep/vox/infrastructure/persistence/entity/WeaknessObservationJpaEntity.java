package com.sep.vox.infrastructure.persistence.entity;

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
    name = "weakness_observation",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_weakness_observation_source",
        columnNames = {
            "source_evaluation_id",
            "framework_criterion_id",
            "sub_attribute",
            "evidence_span"
        }
    ),
    indexes = @Index(
        name = "idx_weakness_observation_student_criterion_time",
        columnList = "student_id, framework_criterion_id, observed_at"
    )
)
public class WeaknessObservationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "source_type", nullable = false, length = 16, updatable = false)
    private String sourceType;

    @Column(name = "source_evaluation_id", nullable = false, updatable = false)
    private UUID sourceEvaluationId;

    @Column(name = "framework_criterion_id", nullable = false, updatable = false)
    private UUID frameworkCriterionId;

    @Column(name = "criterion_code", nullable = false, length = 32, updatable = false)
    private String criterionCode;

    @Column(name = "sub_attribute", nullable = false, length = 64, updatable = false)
    private String subAttribute;

    @Column(name = "evidence_span", nullable = false, length = 200, updatable = false)
    private String evidenceSpan = "";

    @Column(name = "observed_at", nullable = false, updatable = false)
    private OffsetDateTime observedAt;

    protected WeaknessObservationJpaEntity() {
    }

    public WeaknessObservationJpaEntity(
            UUID id,
            UUID studentId,
            String sourceType,
            UUID sourceEvaluationId,
            UUID frameworkCriterionId,
            String criterionCode,
            String subAttribute,
            String evidenceSpan,
            OffsetDateTime observedAt) {
        this.id = id;
        this.studentId = studentId;
        this.sourceType = sourceType;
        this.sourceEvaluationId = sourceEvaluationId;
        this.frameworkCriterionId = frameworkCriterionId;
        this.criterionCode = criterionCode;
        this.subAttribute = subAttribute;
        this.evidenceSpan = evidenceSpan == null ? "" : evidenceSpan;
        this.observedAt = observedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getSourceEvaluationId() {
        return sourceEvaluationId;
    }

    public UUID getFrameworkCriterionId() {
        return frameworkCriterionId;
    }

    public String getCriterionCode() {
        return criterionCode;
    }

    public String getSubAttribute() {
        return subAttribute;
    }

    public String getEvidenceSpan() {
        return evidenceSpan;
    }

    public OffsetDateTime getObservedAt() {
        return observedAt;
    }
}
