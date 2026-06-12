package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSchoolClassUserCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassUserRequest;

public final class CreateSchoolClassUserCommandMapper {

    private CreateSchoolClassUserCommandMapper() {
    }

    public static CreateSchoolClassUserCommand fromRequest(UUID schoolId, UUID classId, CreateSchoolClassUserRequest request) {
        return new CreateSchoolClassUserCommand(schoolId, classId, request.userId());
    }
}
