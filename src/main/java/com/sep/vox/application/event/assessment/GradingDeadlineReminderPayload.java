package com.sep.vox.application.event.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;


public record GradingDeadlineReminderPayload(
    UUID assignmentId,
    UUID teacherId,
    String assessmentName,
    String roundType,
    Instant deadlineAt,
    UUID assessmentId,
    AssessmentKind assessmentKind
) {

}
