package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateQuestionCommand(
    UUID topicId,
    String questionText,
    String audioUrl,
    String difficultyLevel,
    String questionType,
    int durationSeconds
) {
}
