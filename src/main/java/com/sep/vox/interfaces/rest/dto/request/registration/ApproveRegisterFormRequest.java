package com.sep.vox.interfaces.rest.dto.request.registration;

import com.sep.vox.application.port.input.command.registration.ApproveRegisterFormCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApproveRegisterFormRequest(
    @NotNull(message = "Registration form ID must not be blank")
    UUID registerFormId,

    @NotBlank(message = "School code must not be blank")
    @Size(max = 100, message = "School code must not exceed 100 characters")
    String schoolCode,

    @Size(max = 2048, message = "School description must not exceed 2048 characters")
    String description, 

    @NotBlank(message = "Province code must not be blank")
    @Size(max = 100, message = "Province code must not exceed 100 characters")
    String provinceCode
) {

    public static ApproveRegisterFormCommand toCommand(UUID registerFormId, ApproveRegisterFormRequest request) {
        return new ApproveRegisterFormCommand(
            registerFormId,
            request.schoolCode(), 
            request.description(), 
            request.provinceCode() 
        );
    }
}
