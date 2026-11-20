package com.sep.vox.application.event;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * A teacher gives an assignment back. The recipient is <em>the admin who assigned it</em>, not the
 * student: the attempt goes back to the unassigned queue and a coordinator has to handle it again.
 *
 * @param assessmentId the grading screen of a class test sits under assessmentId, that of a centralized assessment does not
 * @param assessmentKind decides which grading screen to open
 */
public record GradingAssignmentDeclinedPayloadV1(
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
