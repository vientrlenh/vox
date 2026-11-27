package com.sep.vox.interfaces.rest.dto.request.schoolclass;

import com.sep.vox.application.port.input.command.schoolclass.CreateSchoolClassCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolClassRequest(
    @NotNull(message = "Language ID must not be blank")
    UUID languageId,

    @NotNull(message = "Grade ID must not be blank")
    UUID schoolGradeId,

    @NotBlank(message = "Class code must not be blank")
    @Size(max = 100, message = "Class code must not exceed 100 characters")
    String code,

    @NotBlank(message = "Class name must not be blank")
    @Size(max = 255, message = "Class name must not exceed 255 characters")
    String name,

    @Size(max = 2048, message = "Description must not exceed 2048 characters")
    String description
) {

    public static CreateSchoolClassCommand toCommand(UUID schoolId, CreateSchoolClassRequest request) {
        return new CreateSchoolClassCommand(
            schoolId,
            request.languageId(),
            request.schoolGradeId(),
            request.code(),
            request.name(),
            request.description()
        );
    }
}
