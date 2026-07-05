package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamPaperSectionDto(
    UUID id,
    UUID paperId,
    int order,
    String title,
    String instruction,
    Integer sectionTimeLimitSeconds
) {
}
