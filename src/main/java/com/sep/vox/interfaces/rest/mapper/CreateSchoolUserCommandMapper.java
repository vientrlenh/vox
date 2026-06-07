package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;

public final class CreateSchoolUserCommandMapper {

    public static CreateSchoolUserCommand fromRequest(UUID schoolId, CreateSchoolUserRequest request) {
        var dateOfBirth = DateMapper.toLocalDate(request.dateOfBirth().strip());
        var startDate = request.startDate() != null ? DateMapper.toLocalDate(request.startDate().strip()) : null;
        var endDate = request.endDate() != null ? DateMapper.toLocalDate(request.endDate().strip()) : null;
        return new CreateSchoolUserCommand(
            schoolId,
            request.email(),
            request.phone(),
            request.fullName(),
            dateOfBirth,
            request.address(),
            request.roleCode(),
            request.studentId(),
            startDate,
            endDate
        );
    }
}
