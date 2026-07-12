package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateSchoolRubricResultBandCommand(
        UUID schoolId,
        UUID resultBandId,
        String name,
        String description,
        BigDecimal scoreMin,
        BigDecimal scoreMax,
        Integer order
) {}