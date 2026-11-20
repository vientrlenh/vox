package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamPaperItemDto(
    UUID id,
    UUID blueprintSlotId,
    UUID sectionId,
    UUID paperId,
    UUID questionId,
    int order,
    BigDecimal weight
) {
}
