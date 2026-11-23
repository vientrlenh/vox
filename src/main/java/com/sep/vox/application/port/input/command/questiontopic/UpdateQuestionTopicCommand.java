package com.sep.vox.application.port.input.command.questiontopic;

import java.util.UUID;

public record UpdateQuestionTopicCommand(
    UUID id,
    String name,
    String description
) {
}
