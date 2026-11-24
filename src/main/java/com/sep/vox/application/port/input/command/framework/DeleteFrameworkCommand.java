package com.sep.vox.application.port.input.command.framework;

import java.util.UUID;

public record DeleteFrameworkCommand(
    UUID frameworkId
) {
}
