package com.sep.vox.application.port.input.command.assessment;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentMemberRole;

public record UpdateAssessmentMemberCommand(
    UUID assessmentId,
    UUID memberId,
    AssessmentMemberRole role
) {
}
