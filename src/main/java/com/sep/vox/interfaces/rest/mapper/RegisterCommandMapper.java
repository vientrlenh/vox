package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.command.RegisterCommand;
import com.sep.vox.application.mapper.common.DateMapper;
import com.sep.vox.interfaces.rest.dto.request.RegisterRequest;

public class RegisterCommandMapper {
    
    public static RegisterCommand fromRequest(RegisterRequest request) {
        var dateOfBirth = DateMapper.toLocalDate(request.dateOfBirth().trim());
        return new RegisterCommand(
            request.contactFullName().trim(), 
            request.identityNumber().trim(), 
            request.contactPhone().trim(), 
            request.contactEmail().trim(), 
            dateOfBirth,
            request.contactAddress().trim(),
            request.schoolDomain().trim(), 
            request.schoolName().trim(),
            request.schoolAddress().trim(), 
            request.postalCode().trim(), 
            request.position().trim(), 
            request.studentCount()
        );
    }
}
