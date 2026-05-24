package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.RegisterCommand;
import com.sep.vox.interfaces.rest.dto.request.RegisterRequest;

public class RegisterCommandMapper {
    
    public static RegisterCommand fromRequest(RegisterRequest request) {
        var dateOfBirth = DateMapper.toLocalDate(request.dateOfBirth().strip());
        return new RegisterCommand(
            request.contactFullName(), 
            request.identityNumber(), 
            request.contactPhone(), 
            request.contactEmail(), 
            dateOfBirth,
            request.contactAddress(),
            request.schoolDomain(), 
            request.schoolName(),
            request.schoolAddress(), 
            request.postalCode(), 
            request.position(), 
            request.studentCount()
        );
    }
}
