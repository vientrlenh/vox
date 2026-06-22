package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolGradeLevelCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolGradeLevelRequest;

import java.util.UUID;

public class CreateSchoolGradeLevelCommandMapper {
    public static CreateSchoolGradeLevelCommand fromRequest(UUID schoolId, CreateSchoolGradeLevelRequest request) {
        return new CreateSchoolGradeLevelCommand(
                schoolId,
                request.code(),
                request.name(),
                request.description(),
                request.order()
        );
    }
}
