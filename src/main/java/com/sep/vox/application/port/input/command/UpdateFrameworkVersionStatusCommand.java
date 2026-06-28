package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkVersionStatus;

public record UpdateFrameworkVersionStatusCommand(
    UUID frameworkId,
    UUID versionId,
    FrameworkVersionStatus status
) {
}
