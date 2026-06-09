package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.application.common.permission.ReviewAction;

public record ReviewQuestionCommand(
    UUID questionId,
    ReviewAction action,
    String note,
    String reason
) {
}
