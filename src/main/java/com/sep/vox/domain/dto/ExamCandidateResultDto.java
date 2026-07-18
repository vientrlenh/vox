package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamCandidateResultDto(
    UUID id,
    UUID examId,
    BigDecimal totalScore,
    String status,
    String releasedAt,
    String finalizedAt,
    String createdAt
) {
}
