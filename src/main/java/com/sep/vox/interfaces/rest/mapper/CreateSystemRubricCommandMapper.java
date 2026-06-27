package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSystemRubricCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemRubricRequest;

public class CreateSystemRubricCommandMapper {

    public static CreateSystemRubricCommand fromRequest(CreateSystemRubricRequest request) {
        return new CreateSystemRubricCommand(
                request.code(),
                request.name(),
                request.description(),
                request.languageId(),
                request.frameworkId()
        );
    }
}