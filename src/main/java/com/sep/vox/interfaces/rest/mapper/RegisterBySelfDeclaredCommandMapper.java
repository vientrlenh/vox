package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.RegisterBySelfDeclaredCommand;
import com.sep.vox.interfaces.rest.dto.request.RegisterBySelfDeclaredRequest;

public final class RegisterBySelfDeclaredCommandMapper {
    

    public static RegisterBySelfDeclaredCommand fromRequest(RegisterBySelfDeclaredRequest request) {
        var dateOfBirth = DateMapper.toLocalDate(request.dateOfBirth().strip());
        return new RegisterBySelfDeclaredCommand(
            request.schoolName(), 
            request.schoolDomain(), 
            request.schoolDistrict(), 
            request.schoolProvince(), request.schoolAddress(), request.contactFullName(), 
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
