package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateQuestionCommand(
    UUID id,
    UUID topicId,
    String questionText,
    String audioUrl,
    UUID standardLevelId,
    String questionType,
    int durationSeconds,
    boolean isActive
) {
}
