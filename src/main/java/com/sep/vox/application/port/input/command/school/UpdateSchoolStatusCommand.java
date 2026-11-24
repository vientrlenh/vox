package com.sep.vox.application.port.input.command.school;

import java.util.UUID;

public record UpdateSchoolStatusCommand(
        UUID id,
        boolean isActive
) {}