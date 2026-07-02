package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateFrameworkRequest;

public final class UpdateFrameworkCommandMapper {

    public static UpdateFrameworkCommand fromRequest(UUID frameworkId, UpdateFrameworkRequest request) {
        return new UpdateFrameworkCommand(frameworkId, request.name(), request.description());
    }
}
