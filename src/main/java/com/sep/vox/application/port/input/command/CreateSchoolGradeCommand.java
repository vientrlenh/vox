package com.sep.vox.application.port.input.command;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSchoolGradeCommand (
    UUID schoolId,
    String code,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate
) {}
