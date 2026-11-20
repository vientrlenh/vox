package com.sep.vox.application.event;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * Reminds a teacher that an assignment is about to be due or already overdue. Each assignment is reminded only once.
 *
 * @param assessmentId the grading screen of a class test sits under assessmentId, that of a centralized assessment does not
 * @param assessmentKind decides which grading screen to open
 */
public record GradingDeadlineReminderPayloadV1(
    UUID assignmentId,
    UUID teacherId,
    String assessmentName,
    String roundType,
    Instant deadlineAt,
    UUID assessmentId,
    AssessmentKind assessmentKind
) {

}
