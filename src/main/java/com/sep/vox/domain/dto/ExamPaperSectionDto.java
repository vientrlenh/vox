package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamPaperSectionDto(
    UUID id,
    UUID paperId,
    int order,
    String title,
    String instruction,
    BigDecimal weight,
    Integer sectionTimeLimitSeconds
) {
}
