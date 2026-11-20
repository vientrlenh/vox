package com.sep.vox.application.event;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * A teacher has just been ASSIGNED a specific grading round: first grading, post-check, re-inspection of an
 * invalidated attempt, or an appeal.
 *
 * <p>Only published when SOMEONE ELSE made the assignment (school admin manual assignment, automatic assignment,
 * reassignment of the grader). A teacher who claims an attempt from the class test queue themself does not trigger it:
 * they have just pressed the button, and reporting back what they have just done is only noise.
 *
 * <p>One event per ASSIGNMENT, unlike {@link AssessmentHumanGradingRequiredPayloadV1} which batches the
 * whole assessment: here every notification is a specific task for one person, so the count equals the
 * number of real tasks rather than multiplying by the number of candidates.
 *
 * <p>Produces an in-app notification ONLY, no email -- it shares the assessment-grading-review topic, where the
 * mail consumer is not registered.
 *
 * @param candidateLabel what the teacher uses to recognize the attempt, ALREADY chosen by the anonymity rule at
 *        publish time: the student's name for a class test, the 8-character attempt code for a centralized assessment.
 *        It is decided here rather than letting the consumer choose from {@code assessmentKind} on purpose --
 *        the blind grading of a centralized assessment must not leak (see GradingStudentIdentityQueryTests),
 *        and the payload stays permanently in the notifications.payload column. Never put
 *        a real name here for a centralized assessment.
 * @param deadlineAt grading deadline, null for a class test (no administrative deadline)
 */
public record GradingAssignmentOpenedPayloadV1(
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
