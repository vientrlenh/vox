package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentMemberRole;

public record CreateAssessmentMemberCommand(
    UUID assessmentId,
    UUID userId,
    AssessmentMemberRole role
) {
}
