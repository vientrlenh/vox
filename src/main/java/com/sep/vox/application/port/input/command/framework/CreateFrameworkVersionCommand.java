package com.sep.vox.application.port.input.command.framework;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateFrameworkVersionCommand(
    UUID frameworkId,
    String name,
    String description,
    Instant effectiveFrom,
    Instant effectiveTo, 
    List<CreateFrameworkCriterionCommand> criteria, 
    List<CreateFrameworkResultBandCommand> resultBands
) {
}
