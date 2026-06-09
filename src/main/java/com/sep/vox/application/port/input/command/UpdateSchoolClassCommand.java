package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateSchoolClassCommand(
        UUID id,
        String name,
        boolean nameProvided,
        String description,
        boolean descriptionProvided,
        String status,
        boolean statusProvided) {
}
