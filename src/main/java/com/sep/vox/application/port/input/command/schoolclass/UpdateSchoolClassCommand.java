package com.sep.vox.application.port.input.command.schoolclass;

import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolClassStatus;

public record UpdateSchoolClassCommand(
        UUID id,
        String name,
        boolean nameProvided,
        String description,
        boolean descriptionProvided,
        SchoolClassStatus status,
        boolean statusProvided) {
}
