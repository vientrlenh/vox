package com.sep.vox.interfaces.rest.dto.request.framework;

import com.sep.vox.application.port.input.command.framework.UpdateFrameworkCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFrameworkRequest(
    @NotBlank(message = "Framework name must not be blank")
    @Size(max = 255, message = "Framework name must not exceed 255 characters")
    String name,

    @Size(max = 2048, message = "Description must not exceed 2048 characters")
    String description
) {

    public static UpdateFrameworkCommand toCommand(UUID frameworkId, UpdateFrameworkRequest request) {
        return new UpdateFrameworkCommand(frameworkId, request.name(), request.description());
    }
}
