package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateSchoolRubricApplicabilityCommand(
        UUID schoolId,
        UUID versionId,
        List<ApplicabilityItemCommand> applicabilities
) {
    public record ApplicabilityItemCommand(
            UUID schoolGradeId,
            UUID schoolClassId,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo
    ) {}
}