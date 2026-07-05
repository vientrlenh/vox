package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateQuestionSelectionSpecCommand(
    String questionType,
    String difficulty,
    String targetBandLevel,
    String skillCode,
    UUID topicId
) {
}
