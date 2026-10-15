package com.sep.vox.interfaces.rest.dto.request.framework;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkResultBandCommand;
import com.sep.vox.interfaces.shared.RequestPatternRegex;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record CreateFrameworkResultBandRequest(
    @NotBlank(message = "Result band code is required")
    @Size(max = 50, message = "Result band code must not exceed 50 characters")
    @Pattern(regexp = RequestPatternRegex.CODE_REGEX, message = "Invalid result band code")
    String code,

    @NotBlank(message = "Result band label is required")
    @Size(max = 100, message = "Result band label must not exceed 100 characters")
    String label,

    @Size(max = 2048, message = "Result band description must not exceed 2048 characters")
    String description, 

    @NotNull(message = "Order is required")
    @Min(value = 1, message = "Order must not be lower than 1")
    @Max(value = Integer.MAX_VALUE, message = "Order must not be greater than " + Integer.MAX_VALUE)
    Integer order
) {

    public static CreateFrameworkResultBandCommand toCommand(CreateFrameworkResultBandRequest request) {
        return new CreateFrameworkResultBandCommand(
            request.code,
            request.label,
            request.description, 
            request.order
        );
    }
}
