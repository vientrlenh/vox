package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AppealCriterionMetaInfo(
    UUID id,
    String code,
    String label,
    String description,
    BigDecimal minScore,
    BigDecimal maxScore
) {
}
