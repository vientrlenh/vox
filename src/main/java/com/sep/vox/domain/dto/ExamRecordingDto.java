package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamRecordingDto(
    UUID id,
    UUID examId,
    String audioUrl,
    Integer durationSeconds,
    String transcript,
    String submittedAt
) {
}
