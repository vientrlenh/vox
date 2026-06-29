package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateSystemRubricCommand(
        UUID rubricId,
        String name,
        String description
) {}