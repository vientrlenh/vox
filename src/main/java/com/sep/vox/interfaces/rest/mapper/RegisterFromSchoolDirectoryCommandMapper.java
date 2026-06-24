package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.RegisterFromSchoolDirectoryCommand;
import com.sep.vox.interfaces.rest.dto.request.RegisterFromSchoolDirectoryRequest;

public final class RegisterFromSchoolDirectoryCommandMapper {
    
    public static RegisterFromSchoolDirectoryCommand fromRequest(RegisterFromSchoolDirectoryRequest request) {
        var dateOfBirth = DateMapper.toLocalDate(request.dateOfBirth().strip());
        return new RegisterFromSchoolDirectoryCommand(
            request.schoolDirectoryId(),
            request.contactFullName(), 
            request.identityNumber(), 
            request.contactPhone(), 
            request.contactEmail(), 
            dateOfBirth,
            request.contactAddress(),
            request.postalCode(), 
            request.position(), 
            request.studentCount(), 
            request.documentUrls()
        );
    }
}
