package com.sep.vox.application.query.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExamItemResponseDto(
    UUID id,
    UUID examId,
    String audioUrl,
    Integer durationSeconds,
    String transcript,
    OffsetDateTime submittedAt
) {
}
