package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubAttributePriority(
    UUID id,
    UUID studentId,
    UUID frameworkCriterionId,
    String subAttribute,
    int frequency,
    int recentFrequency,
    BigDecimal priority,
    boolean practiceable,
    OffsetDateTime computedAt
) {
}
