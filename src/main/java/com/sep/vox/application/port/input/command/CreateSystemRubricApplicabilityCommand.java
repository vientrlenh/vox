package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateSystemRubricApplicabilityCommand(
        UUID versionId,
        List<ApplicabilityItemCommand> applicabilities
) {
    public record ApplicabilityItemCommand(
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo
    ) {}
}