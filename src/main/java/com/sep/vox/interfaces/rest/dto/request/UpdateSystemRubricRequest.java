package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSystemRubricCommand;

import jakarta.validation.constraints.NotBlank;

public record UpdateSystemRubricRequest(
        @NotBlank(message = "Tên bộ Rubric không được để trống")
        String name,

        String description
) {

    public static UpdateSystemRubricCommand toCommand(UUID rubricId, UpdateSystemRubricRequest request) {
        return new UpdateSystemRubricCommand(
                rubricId,
                request.name(),
                request.description()
        );
    }
}
