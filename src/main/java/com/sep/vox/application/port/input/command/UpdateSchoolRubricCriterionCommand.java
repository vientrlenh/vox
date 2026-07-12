package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateSchoolRubricCriterionCommand(
        UUID schoolId,
        UUID criterionId,
        String name,
        String description,
        String examplesJson,
        BigDecimal weight,
        BigDecimal minScore,
        BigDecimal maxScore,
        Integer order,
        Boolean isRequired
) {}