package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.UUID;

public record CreateFrameworkVersionCommand(
    UUID frameworkId,
    String code,
    String name,
    String description,
    int version,
    Instant effectiveFrom,
    Instant effectiveTo
) {
}
