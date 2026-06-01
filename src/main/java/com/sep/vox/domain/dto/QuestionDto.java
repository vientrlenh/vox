package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionDto(
    UUID id,
    UUID topicId,
    String questionText,
    String audioUrl,
    UUID standardLevelId,
    String standardLevelCode,
    String frameworkCode,
    String frameworkName,
    String questionType,
    int durationSeconds,
    boolean isActive,
    String createdAt
) {
}
