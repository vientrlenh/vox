package com.sep.vox.application.port.input.command.registration;

import java.util.UUID;

public record ApproveRequestCommand(
    UUID requestId
) {
}
