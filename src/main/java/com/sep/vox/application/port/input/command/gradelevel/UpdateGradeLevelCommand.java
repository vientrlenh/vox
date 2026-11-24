package com.sep.vox.application.port.input.command.gradelevel;

import java.util.UUID;

public record UpdateGradeLevelCommand(
        UUID gradeLevelId,
        String name,
        String description,
        boolean descriptionProvided,
        Integer order
) {}
