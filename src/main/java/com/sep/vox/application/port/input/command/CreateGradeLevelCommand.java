package com.sep.vox.application.port.input.command;

public record CreateGradeLevelCommand(
        String code,
        String name,
        String description,
        Integer order
) {}
