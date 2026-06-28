package com.sep.vox.application.port.input.command;

public record CreateFrameworkCommand(
    String code,
    String name,
    String description
) {
}
