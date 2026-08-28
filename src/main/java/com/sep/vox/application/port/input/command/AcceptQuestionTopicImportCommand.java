package com.sep.vox.application.port.input.command;

import java.util.Map;
import java.util.UUID;

public record AcceptQuestionTopicImportCommand(
    UUID importSessionId,
    Map<String, String> confirmedMapping
) {
}
