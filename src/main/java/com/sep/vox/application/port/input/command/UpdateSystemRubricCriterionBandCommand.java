package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateSystemRubricCriterionBandCommand(
        UUID bandId,
        String code,
        BigDecimal scoreMin,
        BigDecimal scoreMax
) {}