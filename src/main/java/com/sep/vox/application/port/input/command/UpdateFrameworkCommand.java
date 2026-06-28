package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateFrameworkCommand(
    UUID frameworkId,
    String name,
    String description
) {
}
