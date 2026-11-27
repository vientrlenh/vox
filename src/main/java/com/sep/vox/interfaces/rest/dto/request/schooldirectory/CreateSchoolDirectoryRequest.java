package com.sep.vox.interfaces.rest.dto.request.schooldirectory;

import com.sep.vox.application.port.input.command.schooldirectory.CreateSchoolDirectoryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSchoolDirectoryRequest(
    @NotBlank(message = "School directory code must not be blank")
    @Size(max = 100, message = "School directory code must not exceed 100 characters")
    String code, 

    @NotBlank(message = "School directory name must not be blank")
    @Size(max = 255, message = "School directory name must not exceed 255 characters")
    String name, 

    @NotBlank(message = "Province code of the school directory must not be blank")
    @Size(max = 100, message = "Province code of the school directory must not exceed 100 characters")
    String provinceCode,

    @NotBlank(message = "Province name of the school directory must not be blank")
    @Size(max = 255, message = "Province name of the school directory must not exceed 255 characters")
    String provinceName, 

    @NotBlank(message = "Region name of the school directory must not be blank")
    @Size(max = 255, message = "Region name of the school directory must not exceed 255 characters")
    String districtName, 

    @Size(max = 100, message = "School domain must not exceed 100 characters")
    String domain, 

    @Size(max = 512, message = "Address of the school directory must not exceed 512 characters")
    String address
) {

    public static CreateSchoolDirectoryCommand toCommand(CreateSchoolDirectoryRequest request) {
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
