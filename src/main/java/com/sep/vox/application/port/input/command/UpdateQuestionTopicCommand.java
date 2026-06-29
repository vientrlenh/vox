package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateQuestionTopicCommand(
    UUID id,
    String name,
    String description
) {
}
