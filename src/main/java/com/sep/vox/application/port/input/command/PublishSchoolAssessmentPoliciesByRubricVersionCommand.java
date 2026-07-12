package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record PublishSchoolAssessmentPoliciesByRubricVersionCommand(
        UUID schoolId,
        UUID rubricVersionId
) {
}