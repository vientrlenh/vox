package com.sep.vox.application.port.input.command.auth;

import java.util.UUID;

public record SetUpPasswordCommand(
    UUID userId,
    String token,
    String password
) {
    
}
