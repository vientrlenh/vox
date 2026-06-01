package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateQuestionTopicCommand(
    UUID bankId,
    String topicName,
    String description
) {
}
