package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.RegisterBySelfDeclaredCommand;
import com.sep.vox.interfaces.rest.dto.request.RegisterBySelfDeclaredRequest;

public final class RegisterBySelfDeclaredCommandMapper {
    

    public static RegisterBySelfDeclaredCommand fromRequest(RegisterBySelfDeclaredRequest request) {
        return new RegisterBySelfDeclaredCommand(
            request.schoolName(), 
            request.schoolDomain(), 
            request.schoolDistrict(), 
            request.schoolProvince(), request.schoolAddress(), request.contactFullName(), 
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
