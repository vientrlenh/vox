package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateQuestionCommand(
    UUID id,
    UUID topicId,
    String questionText,
    String audioUrl,
    String difficultyLevel,
    String questionType,
    int durationSeconds,
    boolean isActive
) {
}
