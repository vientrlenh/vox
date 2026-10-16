package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;

public record ViewAssessmentCandidatesQuery(
    UUID assessmentId,
    UUID scheduleId,
    AssessmentCandidateStatus status
) {
}
