package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.RejectRegisterFormCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRegisterFormRequest(
    @NotBlank(message = "Lý do từ chối không được để trống")
    @Size(max = 255, message = "Lý do từ chối không được vượt quá 255 ký tự")
    String reason
) {

    public static RejectRegisterFormCommand toCommand(UUID registerFormId, RejectRegisterFormRequest request) {
        return new RejectRegisterFormCommand(
            registerFormId,
            request.reason()
        );
    }
}
