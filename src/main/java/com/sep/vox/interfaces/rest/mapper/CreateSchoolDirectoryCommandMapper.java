package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolDirectoryCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolDirectoryRequest;

public final class CreateSchoolDirectoryCommandMapper {
    

    public static CreateSchoolDirectoryCommand fromRequest(CreateSchoolDirectoryRequest request) {
        return new CreateSchoolDirectoryCommand(
            request.code(), 
            request.name(), 
            request.provinceCode(), 
            request.provinceName(), 
            request.districtName(), 
            request.domain(), 
            request.address()
        );
    }
}
