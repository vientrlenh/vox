package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AssessmentPaperItemDto(
    UUID id,
    UUID blueprintSlotId,
    UUID sectionId,
    UUID paperId,
    UUID questionId,
    int order,
    BigDecimal weight
) {
}
