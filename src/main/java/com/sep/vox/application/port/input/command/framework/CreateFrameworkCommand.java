package com.sep.vox.application.port.input.command.framework;

public record CreateFrameworkCommand(
    String code,
    String name,
    String description,
    boolean isActive
) {
}
