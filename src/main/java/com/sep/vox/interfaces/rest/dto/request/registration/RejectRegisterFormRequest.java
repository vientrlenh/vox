package com.sep.vox.interfaces.rest.dto.request.registration;

import com.sep.vox.application.port.input.command.registration.RejectRegisterFormCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRegisterFormRequest(
    @NotBlank(message = "Rejection reason must not be blank")
    @Size(max = 255, message = "Rejection reason must not exceed 255 characters")
    String reason
) {

    public static RejectRegisterFormCommand toCommand(UUID registerFormId, RejectRegisterFormRequest request) {
        return new RejectRegisterFormCommand(
            registerFormId,
            request.reason()
        );
    }
}
