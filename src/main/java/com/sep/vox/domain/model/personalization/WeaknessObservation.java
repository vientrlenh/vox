package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WeaknessObservation(
    UUID id,
    UUID studentId,
    WeaknessObservationSourceType sourceType,
    UUID sourceEvaluationId,
    UUID frameworkCriterionId,
    String criterionCode,
    String subAttribute,
    String evidenceSpan,
    OffsetDateTime observedAt
) {
    public WeaknessObservation(
            UUID studentId,
            WeaknessObservationSourceType sourceType,
            UUID sourceEvaluationId,
            UUID frameworkCriterionId,
            String criterionCode,
            String subAttribute,
            String evidenceSpan,
            OffsetDateTime observedAt) {
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
