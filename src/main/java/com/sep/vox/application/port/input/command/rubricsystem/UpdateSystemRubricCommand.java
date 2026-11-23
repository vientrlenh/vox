package com.sep.vox.application.port.input.command.rubricsystem;

import java.util.UUID;

public record UpdateSystemRubricCommand(
        UUID rubricId,
        String name,
        String description
) {}