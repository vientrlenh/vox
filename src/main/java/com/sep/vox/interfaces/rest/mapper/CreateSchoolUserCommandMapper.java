package com.sep.vox.interfaces.rest.mapper;

import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;

public final class CreateSchoolUserCommandMapper {

    public static CreateSchoolUserCommand fromRequest(UUID schoolId, CreateSchoolUserRequest request) {
        LocalDate dateOfBirth = DateMapper.toLocalDate(request.dateOfBirth().strip());
        LocalDate startDate = request.startDate() != null ? DateMapper.toLocalDate(request.startDate().strip()) : null;
        LocalDate endDate = request.endDate() != null ? DateMapper.toLocalDate(request.endDate().strip()) : null;
        return new CreateSchoolUserCommand(
            schoolId,
            request.email(),
            request.phone(),
            request.fullName(),
            dateOfBirth,
            request.address(),
            request.roleCode(),
            startDate,
            endDate
        );
    }
}
