package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateQuestionTopicCommand(
    UUID id,
    UUID bankId,
    String topicName,
    String description
) {
}
