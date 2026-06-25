package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateFrameworkCommand(
    UUID frameworkId,
    String code,
    String name,
    String description
) {
}
