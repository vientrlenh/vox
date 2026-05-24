package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRequest;

public final class CreateSchoolCommandMapper {
    
    public static CreateSchoolCommand fromRequest(CreateSchoolRequest request) {
        return new CreateSchoolCommand(
            request.code(), 
            request.name(), 
            request.description(), 
            request.contactPhone(), 
            request.contactEmail(), 
            request.domain(), 
            request.address(), 
            request.studentCount()
        );
    }
}
