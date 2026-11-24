package com.sep.vox.interfaces.rest.dto.request.schoolclassuser;

import com.sep.vox.application.port.input.command.schoolclassuser.CreateSchoolClassUserCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateSchoolClassUserRequest(
    @NotNull(message = "ID người dùng không được để trống")
    UUID userId
) {

    public static CreateSchoolClassUserCommand toCommand(UUID schoolId, UUID classId, CreateSchoolClassUserRequest request) {
        return new CreateSchoolClassUserCommand(schoolId, classId, request.userId());
    }
}
