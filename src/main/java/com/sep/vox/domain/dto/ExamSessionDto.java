package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamSessionDto(
    UUID id,
    UUID examId,
    UUID candidateId,
    UUID paperId,
    String startedAt,
    String submittedAt,
    String status
) {
}
