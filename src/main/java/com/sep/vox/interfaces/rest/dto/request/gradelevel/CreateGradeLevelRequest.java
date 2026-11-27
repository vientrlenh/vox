package com.sep.vox.interfaces.rest.dto.request.gradelevel;

import com.sep.vox.application.port.input.command.gradelevel.CreateGradeLevelCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateGradeLevelRequest(
        @NotBlank(message = "Grade code must not be blank")
        String code,

        @NotBlank(message = "Grade name must not be blank")
        String name,

        String description,

        @NotNull(message = "Grade order must not be blank")
        @Positive(message = "Grade order must be greater than 0")
        Integer order
) {

    public static CreateGradeLevelCommand toCommand(CreateGradeLevelRequest request) {
        return new CreateGradeLevelCommand(
                request.code(),
                request.name(),
                request.description(),
                request.order()
        );
    }
}
