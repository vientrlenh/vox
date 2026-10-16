package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkVersionStatusCommand;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateFrameworkVersionStatusRequest(
    @NotNull(message = "Trạng thái không được để trống")
    FrameworkVersionStatus status
) {

    public static UpdateFrameworkVersionStatusCommand toCommand(UUID frameworkId, UUID versionId, UpdateFrameworkVersionStatusRequest request) {
        return new UpdateFrameworkVersionStatusCommand(frameworkId, versionId, request.status());
    }
}
