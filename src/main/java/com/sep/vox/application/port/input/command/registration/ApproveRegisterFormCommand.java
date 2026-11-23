package com.sep.vox.application.port.input.command.registration;

import java.util.UUID;

public record ApproveRegisterFormCommand(
    UUID registerFormId,
    String schoolCode, 
    String description, 
    String schoolProvinceCode
) {
    
}
