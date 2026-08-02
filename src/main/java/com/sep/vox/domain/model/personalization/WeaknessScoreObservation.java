package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WeaknessScoreObservation(
    UUID studentId,
    UUID frameworkCriterionId,
    String criterionCode,
    BigDecimal finalScore,
    BigDecimal minScore,
    BigDecimal maxScore,
    OffsetDateTime evaluatedAt,
    String sourceType,
    UUID evaluationId,
    UUID schoolClassId,
    UUID schoolGradeId
) {
}
