package com.sep.vox.application.event.assessment;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

public record AssessmentGradingAssignmentDeclinedPayload(
    UUID assignmentId,
    UUID candidateResultId,
    UUID teacherId,
    UUID assignedBy,
    String assessmentName,
    String reason,
    UUID assessmentId,
    AssessmentKind assessmentKind
) {

}
