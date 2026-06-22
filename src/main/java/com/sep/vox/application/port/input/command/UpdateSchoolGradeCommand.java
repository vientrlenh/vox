package com.sep.vox.application.port.input.command;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateSchoolGradeCommand(
        UUID schoolGradeId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {}
