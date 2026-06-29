package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateSchoolRubricCriterionBandCommand(
        UUID schoolId,
        UUID bandId,
        String code,
        BigDecimal scoreMin,
        BigDecimal scoreMax
) {}