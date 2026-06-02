package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateSchoolClassCommand(
        UUID id,
        String name,
        String description,
        UUID targetSchoolLevelVersionId,
        String status) {
}
