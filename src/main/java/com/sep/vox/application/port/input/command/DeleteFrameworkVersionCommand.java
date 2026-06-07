package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteFrameworkVersionCommand(
    UUID frameworkId,
    UUID versionId
) {
}
