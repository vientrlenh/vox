package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record RejectRegisterFormCommand(
    UUID registerFormId,
    String reason
) {
    
}
