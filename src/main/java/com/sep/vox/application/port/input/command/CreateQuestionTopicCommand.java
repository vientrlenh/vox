package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateQuestionTopicCommand(
    UUID questionBankId,
    String code,
    String name,
    String description
) {
}
