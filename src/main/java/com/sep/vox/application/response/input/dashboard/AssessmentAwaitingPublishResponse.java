package com.sep.vox.application.response.input.dashboard;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.query.dto.AssessmentAwaitingPublishDto;

/** An assessment that has closed but still has attempts without a score -- publishing permanently closes every exit for those attempts. */
public record AssessmentAwaitingPublishResponse(
    UUID assessmentId,
    String code,
    String name,
    /** null for an assessment with no close time. */
    Instant closeAt,
    int unscoredCount,
    int aiFailedRetryLeft,
    int aiFailedNoRetryLeft,
    int awaitingHumanGrading
) {

    public static AssessmentAwaitingPublishResponse of(AssessmentAwaitingPublishDto dto) {
        return new AssessmentAwaitingPublishResponse(
            dto.assessmentId(),
            dto.code(),
            dto.name(),
            dto.closeAt(),
            dto.unscoredCount(),
            dto.aiFailedRetryLeft(),
            dto.aiFailedNoRetryLeft(),
            dto.awaitingHumanGrading()
        );
    }
}
