package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.RegisterFromSchoolDirectoryCommand;
import com.sep.vox.interfaces.rest.dto.request.RegisterFromSchoolDirectoryRequest;

public final class RegisterFromSchoolDirectoryCommandMapper {
    
    public static RegisterFromSchoolDirectoryCommand fromRequest(RegisterFromSchoolDirectoryRequest request) {
        return new RegisterFromSchoolDirectoryCommand(
            request.schoolDirectoryId(),
            request.contactFullName(), 
            request.identityNumber(), 
            request.contactPhone(), 
            request.contactEmail(), 
            request.dateOfBirth(),
            request.contactAddress(),
            request.postalCode(), 
            request.position(), 
            request.studentCount(), 
            request.documentUrls()
        );
    }
}
