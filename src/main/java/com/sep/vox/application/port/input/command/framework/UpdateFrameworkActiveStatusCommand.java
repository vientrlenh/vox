package com.sep.vox.application.port.input.command.framework;

import java.util.UUID;

public record UpdateFrameworkActiveStatusCommand(
    UUID frameworkId, 
    boolean isActive
) {
}
