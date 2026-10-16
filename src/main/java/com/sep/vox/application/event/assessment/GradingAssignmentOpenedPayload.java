package com.sep.vox.application.event.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

public record GradingAssignmentOpenedPayload(
    UUID assignmentId,
    UUID teacherId,
    UUID assessmentId,
    String assessmentName,
    AssessmentKind assessmentKind,
    String roundType,
    String candidateLabel,
    Instant deadlineAt
) {

}
