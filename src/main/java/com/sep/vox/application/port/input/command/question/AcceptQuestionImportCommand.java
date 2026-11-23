package com.sep.vox.application.port.input.command.question;

import java.util.Map;
import java.util.UUID;

public record AcceptQuestionImportCommand(
    UUID importSessionId,
    Map<String, String> confirmedMapping
) {
}
