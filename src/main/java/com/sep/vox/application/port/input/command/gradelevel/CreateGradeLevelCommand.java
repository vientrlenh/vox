package com.sep.vox.application.port.input.command.gradelevel;

// No schoolId any more: a grade level is a catalog shared across the whole system, only a system admin can create it.
public record CreateGradeLevelCommand(
        String code,
        String name,
        String description,
        Integer order
) {}
