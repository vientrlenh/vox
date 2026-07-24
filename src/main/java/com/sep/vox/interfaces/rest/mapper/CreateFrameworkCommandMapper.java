package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateFrameworkCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkRequest;

public final class CreateFrameworkCommandMapper {

    public static CreateFrameworkCommand fromRequest(CreateFrameworkRequest request) {
        return new CreateFrameworkCommand(
            request.code(),
            request.name(),
            request.description(),
            request.isActive() != null && request.isActive());
    }
}
