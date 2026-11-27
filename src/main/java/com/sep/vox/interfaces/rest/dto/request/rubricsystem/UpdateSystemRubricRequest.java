package com.sep.vox.interfaces.rest.dto.request.rubricsystem;

import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;

public record UpdateSystemRubricRequest(
        @NotBlank(message = "Rubric name must not be blank")
        String name,

        String description
) {

    public static UpdateSystemRubricCommand toCommand(UUID rubricId, UpdateSystemRubricRequest request) {
        return new UpdateSystemRubricCommand(
                rubricId,
                request.name(),
                request.description(),
                request.description() != null
        );
    }
}
