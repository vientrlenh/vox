package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSchoolDirectoryCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolDirectoryRequest(
    @NotBlank(message = "School code is required")
    @Size(max = 100, message = "School code must not exceed 100 characters")
    String code, 

    @NotBlank(message = "School name is required")
    @Size(max = 255, message = "School name must not exceed 255 characters")
    String name, 

    @NotNull(message = "School ward ID is required")
    UUID wardId,

    UUID cityId,

    @NotNull(message = "School province ID is required")
    UUID provinceId,

    @Size(max = 100, message = "School domain must not exceed 100 characters")
    String domain, 

    @NotBlank(message = "School street address is required")
    @Size(max = 512, message = "School street address must not exceed 255 characters")
    String streetAddress
) {
    
    public static CreateSchoolDirectoryCommand toCommand(CreateSchoolDirectoryRequest request) {
        return new CreateSchoolDirectoryCommand(
            request.code, 
            request.name, 
            request.wardId, 
            request.cityId, 
            request.provinceId, 
            request.domain, 
            request.streetAddress
        );
    }
}
