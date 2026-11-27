package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * An assessment that has CLOSED but still has attempts without a score.
 *
 * <p>The reason it is a separate card rather than a row in the assessment list: publishing scores is a ONE-WAY
 * door. {@code RetryGradingAssessmentSessionUseCase} and {@code HandOffGradingToHumanUseCase} both
 * refuse when the assessment is in {@code RESULTS_PUBLISHED}, so pressing publish while attempts are still empty permanently
 * closes the books on exactly those attempts -- and no screen warns about this beforehand.
 */
public record AssessmentAwaitingPublishDto(
    UUID assessmentId,
    String code,
    String name,
    Instant closeAt,
    int unscoredCount,
    /** Of those, the ones that can still be rescued by the AI. */
    int aiFailedRetryLeft,
    /** Of those, the ones out of AI attempts -- a teacher MUST be assigned to grade them. */
    int aiFailedNoRetryLeft,
    /** Of those, the ones already in the grader queue (waiting for assignment or being graded). */
    int awaitingHumanGrading
) {
}
