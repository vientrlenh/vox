package com.sep.vox.application.port.input.command.questiontopic;

import java.util.UUID;

public record UpdateQuestionTopicStatusCommand(
    UUID id,
    String action
) {
}
