package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UserQuotaAmount(
    UUID userId,
    Integer amount
) {
}
