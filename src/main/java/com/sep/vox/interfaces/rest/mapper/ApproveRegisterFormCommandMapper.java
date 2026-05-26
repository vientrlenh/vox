package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.ApproveRegisterFormCommand;
import com.sep.vox.interfaces.rest.dto.request.ApproveRegisterFormRequest;

public final class ApproveRegisterFormCommandMapper {
    
    public static ApproveRegisterFormCommand fromRequest(UUID registerFormId, ApproveRegisterFormRequest request) {
        var dateOfBirth = DateMapper.toLocalDate(request.dateOfBirth().strip());
        return new ApproveRegisterFormCommand(
            registerFormId,
            request.schoolCode(), 
            request.schoolName(), 
            request.description(), 
            request.contactPhone(), 
            request.contactEmail(), 
            request.schoolDomain(), 
            request.schoolAddress(), 
            request.studentCount(), 
            request.contactFullName(), 
            dateOfBirth, 
            request.contactAddress()
        );
    }
}
