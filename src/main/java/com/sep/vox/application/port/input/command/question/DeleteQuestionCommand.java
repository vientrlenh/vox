package com.sep.vox.application.port.input.command.question;

import java.util.UUID;

public record DeleteQuestionCommand(UUID questionId) {
}
