package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ApproveRequestCommand(
    UUID requestId,
    String idempotencyKey
) {
}
