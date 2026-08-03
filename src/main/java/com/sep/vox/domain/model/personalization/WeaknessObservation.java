package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

public class WeaknessObservation {

    private UUID id;
    private UUID studentId;
    private WeaknessObservationSourceType sourceType;
    private UUID sourceEvaluationId;
    private UUID frameworkCriterionId;
    private String criterionCode;
    private String subAttribute;
    private String evidenceSpan;
    private Instant observedAt;

    public WeaknessObservation() {
    }

    public WeaknessObservation(
            UUID id,
            UUID studentId,
            WeaknessObservationSourceType sourceType,
            UUID sourceEvaluationId,
            UUID frameworkCriterionId,
            String criterionCode,
            String subAttribute,
            String evidenceSpan,
            Instant observedAt) {
        this.id = id;
        this.studentId = studentId;
        this.sourceType = sourceType;
        this.sourceEvaluationId = sourceEvaluationId;
        this.frameworkCriterionId = frameworkCriterionId;
        this.criterionCode = criterionCode;
        this.subAttribute = subAttribute;
        this.evidenceSpan = evidenceSpan;
        this.observedAt = observedAt;
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

    public WeaknessObservationSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(WeaknessObservationSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceEvaluationId() {
        return sourceEvaluationId;
    }

    public void setSourceEvaluationId(UUID sourceEvaluationId) {
        this.sourceEvaluationId = sourceEvaluationId;
    }

    public UUID getFrameworkCriterionId() {
        return frameworkCriterionId;
    }

    public void setFrameworkCriterionId(UUID frameworkCriterionId) {
        this.frameworkCriterionId = frameworkCriterionId;
    }

    public String getCriterionCode() {
        return criterionCode;
    }

    public void setCriterionCode(String criterionCode) {
        this.criterionCode = criterionCode;
    }

    public String getSubAttribute() {
        return subAttribute;
    }

    public void setSubAttribute(String subAttribute) {
        this.subAttribute = subAttribute;
    }

    public String getEvidenceSpan() {
        return evidenceSpan;
    }

    public void setEvidenceSpan(String evidenceSpan) {
        this.evidenceSpan = evidenceSpan;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }

    public WeaknessObservation(
            UUID studentId,
            WeaknessObservationSourceType sourceType,
            UUID sourceEvaluationId,
            UUID frameworkCriterionId,
            String criterionCode,
            String subAttribute,
            String evidenceSpan,
            Instant observedAt) {
        this(
            UUID.randomUUID(),
            studentId,
            sourceType,
            sourceEvaluationId,
            frameworkCriterionId,
            criterionCode,
            subAttribute,
            evidenceSpan,
            observedAt
        );
    }
}
