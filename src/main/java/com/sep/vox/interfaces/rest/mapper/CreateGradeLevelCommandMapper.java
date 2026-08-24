package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateGradeLevelCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateGradeLevelRequest;

public final class CreateGradeLevelCommandMapper {

    private CreateGradeLevelCommandMapper() {}

    public static CreateGradeLevelCommand fromRequest(CreateGradeLevelRequest request) {
        return new CreateGradeLevelCommand(
                request.code(),
                request.name(),
                request.description(),
                request.order()
        );
    }
}
