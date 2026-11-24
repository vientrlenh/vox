package com.sep.vox.application.port.input.command.schoolgrade;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateSchoolGradeCommand(
        UUID schoolGradeId,
        String name,
        String description,
        boolean descriptionProvided,
        LocalDate startDate,
        LocalDate endDate
) {}
