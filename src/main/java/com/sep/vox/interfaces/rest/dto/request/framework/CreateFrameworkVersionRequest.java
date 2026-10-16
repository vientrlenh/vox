package com.sep.vox.interfaces.rest.dto.request.framework;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkVersionCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFrameworkVersionRequest(
    @NotBlank(message = "Framework name is required")
    @Size(max = 255, message = "Framework name must not exceed 255 characters")
    String name,

    @Size(max = 2048, message = "Description must not exceed 2048 characters")
    String description,

    @NotNull(message = "Effective from is required")
    Instant effectiveFrom,

    Instant effectiveTo,

    @NotNull(message = "Criteria is required")
    List<@NotNull(message = "Criterion must not be null") @Valid CreateFrameworkCriterionRequest> criteria,

    @NotNull(message = "Result bands is required")
    List<@NotNull(message = "Result band must not be null") @Valid CreateFrameworkResultBandRequest> resultBands
) {

    public static CreateFrameworkVersionCommand toCommand(UUID frameworkId, CreateFrameworkVersionRequest request) {
        return new CreateFrameworkVersionCommand(
            frameworkId,
            request.name(),
            request.description(),
            request.effectiveFrom(),
            request.effectiveTo(),
            request.criteria().stream()
                .map(CreateFrameworkCriterionRequest::toCommand)
                .toList(),
            request.resultBands().stream()
                .map(CreateFrameworkResultBandRequest::toCommand)
                .toList()
        );
    }
}
