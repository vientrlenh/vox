package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateFrameworkVersionCommand(
    UUID frameworkId,
    String code,
    String name,
    String description,
    int version,
    OffsetDateTime effectiveFrom,
    OffsetDateTime effectiveTo
) {
}
