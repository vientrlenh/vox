package com.sep.vox.domain.dto.personalization;

import java.util.UUID;

public record PracticeSessionDto(
    UUID id,
    UUID paperId,
    UUID topicId,
    String topicName,
    String origin,
    String status,
    String abandonDiagnosis,
    Double overallScore,
    int gradedSeconds,
    String startedAt,
    String endedAt
) {
}
