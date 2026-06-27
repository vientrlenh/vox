package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolRubricCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRubricRequest;
import java.util.UUID;

public class CreateSchoolRubricCommandMapper {
    public static CreateSchoolRubricCommand fromRequest(UUID schoolId, CreateSchoolRubricRequest request) {
        return new CreateSchoolRubricCommand(
                schoolId,
                request.code(),
                request.name(),
                request.description(),
                request.languageId(),
                request.frameworkId()
        );
    }
}