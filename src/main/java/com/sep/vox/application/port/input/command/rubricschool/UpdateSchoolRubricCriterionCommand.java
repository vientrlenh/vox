package com.sep.vox.application.port.input.command.rubricschool;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateSchoolRubricCriterionCommand(
        UUID schoolId,
        UUID criterionId,
        String name,
        String description,
        boolean descriptionProvided,
        String examplesJson,
        boolean examplesJsonProvided,
        BigDecimal weight,
        BigDecimal minScore,
        BigDecimal maxScore,
        Integer order,
        Boolean isRequired
) {}