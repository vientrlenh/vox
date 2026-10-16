package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSchoolClassUserCommand;

import jakarta.validation.constraints.NotNull;

public record CreateSchoolClassUserRequest(
    @NotNull(message = "User ID is required")
    UUID userId
) {

    public static CreateSchoolClassUserCommand toCommand(UUID schoolId, UUID schoolClassId, CreateSchoolClassUserRequest request) {
        return new CreateSchoolClassUserCommand(schoolId, schoolClassId, request.userId);
    }
}
