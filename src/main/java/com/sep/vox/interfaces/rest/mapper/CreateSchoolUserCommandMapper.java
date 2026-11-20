package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;

public final class CreateSchoolUserCommandMapper {

    public static CreateSchoolUserCommand fromRequest(UUID schoolId, CreateSchoolUserRequest request) {
        return new CreateSchoolUserCommand(
            schoolId,
            request.email(),
            request.phone(),
            request.fullName(),
            request.dateOfBirth(),
            request.address(),
            request.roleCode(),
            request.startDate(),
            request.endDate()
        );
    }
}
