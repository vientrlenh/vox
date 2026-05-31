package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionDto(
    UUID id,
    UUID topicId,
    String questionText,
    String audioUrl,
    String difficultyLevel,
    String questionType,
    int durationSeconds,
    boolean isActive,
    String createdAt
) {
}
