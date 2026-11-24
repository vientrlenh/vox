package com.sep.vox.application.port.input.command.registration;

import java.util.UUID;

public record RejectRegisterFormCommand(
    UUID registerFormId,
    String reason
) {
    
}
