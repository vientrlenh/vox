package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CreateSchoolCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRequest;

public final class CreateSchoolCommandMapper {
    
    public static CreateSchoolCommand fromRequest(CreateSchoolRequest request) {
        var adminDateOfBirth = DateMapper.toLocalDate(request.adminDateOfBirth());
        return new CreateSchoolCommand(
            request.schoolDirectoryId(), 
            request.schoolCode(), 
            request.schoolName(), 
            request.schoolAddress(), 
            request.schoolDomain(), 
            request.studentCount(), 
            request.adminEmail(),
            request.adminPhone(),
            request.adminFullName(),
            adminDateOfBirth,
            request.adminAddress(), 
            request.adminAvatarUrl()
        );
    }
}
