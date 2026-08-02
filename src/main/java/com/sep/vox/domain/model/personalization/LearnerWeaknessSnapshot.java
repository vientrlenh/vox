package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LearnerWeaknessSnapshot(
    UUID id,
    UUID studentId,
    UUID frameworkCriterionId,
    BigDecimal relEstimate,
    BigDecimal weakness,
    int observationCount,
    boolean reliable,
    OffsetDateTime computedAt
) {
}
