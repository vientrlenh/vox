package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.UUID;

public record UpdateFrameworkVersionCommand(
    UUID frameworkId,
    UUID versionId,
    String code,
    String name,
    String description,
    Instant effectiveFrom,
    Instant effectiveTo
) {}
