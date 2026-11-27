package com.sep.vox.interfaces.rest.dto.request.framework;

import com.sep.vox.application.port.input.command.framework.UpdateFrameworkVersionStatusCommand;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateFrameworkVersionStatusRequest(
    @NotNull(message = "Status must not be blank")
    FrameworkVersionStatus status
) {

    public static UpdateFrameworkVersionStatusCommand toCommand(UUID frameworkId, UUID versionId,
            UpdateFrameworkVersionStatusRequest request) {
        return new UpdateFrameworkVersionStatusCommand(frameworkId, versionId, request.status());
    }
}
