package com.sep.vox.interfaces.rest.dto.request.framework;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkVersionCommand;
import java.util.UUID;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateFrameworkVersionRequest(
    @NotBlank(message = "Version code must not be blank")
    @Size(max = 100, message = "Version code must not exceed 100 characters")
    String code,

    @NotBlank(message = "Version name must not be blank")
    @Size(max = 255, message = "Version name must not exceed 255 characters")
    String name,

    @Size(max = 2048, message = "Description must not exceed 2048 characters")
    String description,

    @Min(value = 1, message = "Version number must be greater than 0")
    int version,

    @NotNull(message = "Effective date must not be blank")
    Instant effectiveFrom,

    Instant effectiveTo
) {

    public static CreateFrameworkVersionCommand toCommand(UUID frameworkId, CreateFrameworkVersionRequest request) {
        return new CreateFrameworkVersionCommand(
            frameworkId,
            request.code(),
            request.name(),
            request.description(),
            request.version(),
            request.effectiveFrom(),
            request.effectiveTo()
        );
    }
}
