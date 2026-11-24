package com.sep.vox.application.port.input.command.rubricsystem;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateSystemRubricResultBandCommand(
        UUID resultBandId,
        String name,
        String description,
        boolean descriptionProvided,
        BigDecimal scoreMin,
        BigDecimal scoreMax,
        Integer order
) {}