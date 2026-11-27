package com.sep.vox.interfaces.rest.dto.request.framework;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFrameworkRequest(
    @NotBlank(message = "Framework code must not be blank")
    @Size(max = 100, message = "Framework code must not exceed 100 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Framework code may only contain uppercase letters, digits, underscores and hyphens")
    String code,

    @NotBlank(message = "Framework name must not be blank")
    @Size(max = 255, message = "Framework name must not exceed 255 characters")
    String name,

    @Size(max = 2048, message = "Description must not exceed 2048 characters")
    String description,

    Boolean isActive
) {

    public static CreateFrameworkCommand toCommand(CreateFrameworkRequest request) {
        return new CreateFrameworkCommand(
            request.code(),
            request.name(),
            request.description(),
            request.isActive() != null && request.isActive());
    }
}
