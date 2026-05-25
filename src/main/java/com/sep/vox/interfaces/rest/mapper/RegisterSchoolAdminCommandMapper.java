package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.RegisterSchoolAdminCommand;
import com.sep.vox.interfaces.rest.dto.request.RegisterSchoolAdminRequest;

public final class RegisterSchoolAdminCommandMapper {
    
    public static RegisterSchoolAdminCommand fromRequest(RegisterSchoolAdminRequest request) {
        return new RegisterSchoolAdminCommand(
            request.email(), 
            request.phone(), 
            request.fullName(), 
            DateMapper.toLocalDate(request.dateOfBirth().strip()), 
            request.address(), 
            request.schoolId()
        );
    }
}
