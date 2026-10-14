package com.sep.vox.interfaces.rest.dto.request.framework;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkCriterionCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFrameworkCriterionRequest(
    @NotBlank(message = "Criterion code is required")
    @Size(max = 50, message = "Criterion code must not exceed 50 characters")
    String code,

    @NotBlank(message = "Criterion name is required")
    @Size(max = 255, message = "Criterion name must not exceed 255 characters")
    String name,

    @Size(max = 2048, message = "Criterion description must not exceed 2048 characters")
    String description
) {

    public static CreateFrameworkCriterionCommand toCommand(CreateFrameworkCriterionRequest request) {
        return new CreateFrameworkCriterionCommand(
            request.code(),
            request.name(),
            request.description()
        );
    }
}
