package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.domain.model.language.LearningLanguage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolClassRequest(
    @NotNull(message = "Learning language is required")
    LearningLanguage language,

    @NotNull(message = "School grade ID is required")
    UUID schoolGradeId,

    @NotBlank(message = "School class code is required")
    @Size(max = 100, message = "School class code must not exceed 100 characters")
    String code,

    @NotBlank(message = "School class name is required")
    @Size(max = 255, message = "School class name must not exceed 255 characters")
    String name,

    @Size(max = 2048, message = "School class description must not exceed 2048 characters")
    String description
) {

    public static CreateSchoolClassCommand toCommand(UUID schoolId, CreateSchoolClassRequest request) {
        return new CreateSchoolClassCommand(schoolId, request.language, request.schoolGradeId, request.code, request.name, request.description);
    }
}
