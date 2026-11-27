package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A rubric criterion with its real score range. The FE builds the input boxes from the min/max here
 * instead of hardcoding 0-9 -- every rubric has a different scale.
 */
public record GradingCriterionMetaInfo(
    UUID id,
    String code,
    String label,
    String description,
    BigDecimal minScore,
    BigDecimal maxScore,
    BigDecimal weight,
    boolean required
) {
}
