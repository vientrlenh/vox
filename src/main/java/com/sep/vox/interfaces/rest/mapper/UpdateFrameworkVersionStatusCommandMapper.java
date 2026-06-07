package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkVersionStatusCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateFrameworkVersionStatusRequest;

public final class UpdateFrameworkVersionStatusCommandMapper {

    public static UpdateFrameworkVersionStatusCommand fromRequest(UUID frameworkId, UUID versionId,
            UpdateFrameworkVersionStatusRequest request) {
        return new UpdateFrameworkVersionStatusCommand(frameworkId, versionId, request.status());
    }
}
