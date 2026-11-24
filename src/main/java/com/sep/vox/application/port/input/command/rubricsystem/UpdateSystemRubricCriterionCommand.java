package com.sep.vox.application.port.input.command.rubricsystem;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateSystemRubricCriterionCommand(
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