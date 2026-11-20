package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * One failed grading session in the list of a group.
 *
 * <p>NO score: a {@code GRADING_FAILED} session has no {@code assessment_candidate_results} row to take a score
 * from -- see the javadoc of {@code HandOffGradingToHumanUseCase}. What replaces it are the fields that
 * help triage: how many times it was tried, whether it can be regraded, whether someone has taken it.
 *
 * @param schoolName  {@code null} for a system-level assessment ({@code assessments.school_id} allows null)
 * @param retryable   the assessment has not published scores, i.e. {@code RetryGradingAssessmentSessionUseCase} will accept it;
 *                    one already in {@code RESULTS_PUBLISHED} makes it throw
 * @param handedOff   there is already a result row pointing back to this session, i.e. the attempt HAS been pushed to the grader
 *                    queue. The session stays {@code GRADING_FAILED} because the hand-off deliberately does not change the status,
 *                    so without reading this flag a bulk operation would grade over an attempt a teacher
 *                    is halfway through grading
 */
public record GradingFailureSessionDto(
    UUID sessionId,
    UUID schoolId,
    String schoolName,
    String schoolCode,
    UUID assessmentId,
    String assessmentName,
    String candidateName,
    Instant failedAt,
    Integer retryCount,
    String error,
    boolean retryable,
    boolean handedOff
) {
}
