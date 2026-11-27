package com.sep.vox.interfaces.rest.dto.request.rubricsystem;


import com.sep.vox.application.port.input.command.rubricsystem.CreateSystemRubricCommand;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateSystemRubricRequest(
        @NotBlank(message = "Rubric code must not be blank")
        @Size(max = 50, message = "Rubric code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Rubric name must not be blank")
        @Size(max = 255, message = "Rubric name must not exceed 255 characters")
        String name,

        @Size(max = 2048, message = "Description must not exceed 2048 characters")
        String description,

        @NotNull(message = "Language must not be blank")
        UUID languageId,

        @NotNull(message = "Framework must not be blank")
        UUID frameworkId
) {

    public static CreateSystemRubricCommand toCommand(CreateSystemRubricRequest request) {
        return new CreateSystemRubricCommand(
                request.code(),
                request.name(),
                request.description(),
                request.languageId(),
                request.frameworkId()
        );
    }
}
