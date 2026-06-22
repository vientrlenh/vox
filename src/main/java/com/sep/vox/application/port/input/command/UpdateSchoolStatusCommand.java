package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateSchoolStatusCommand(
        UUID id,
        boolean isActive
) {}