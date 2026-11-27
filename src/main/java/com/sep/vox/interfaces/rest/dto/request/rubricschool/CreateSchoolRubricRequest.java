package com.sep.vox.interfaces.rest.dto.request.rubricschool;

import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateSchoolRubricRequest(
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

    public static CreateSchoolRubricCommand toCommand(UUID schoolId, CreateSchoolRubricRequest request) {
        return new CreateSchoolRubricCommand(
                schoolId,
                request.code(),
                request.name(),
                request.description(),
                request.languageId(),
                request.frameworkId()
        );
    }
}
