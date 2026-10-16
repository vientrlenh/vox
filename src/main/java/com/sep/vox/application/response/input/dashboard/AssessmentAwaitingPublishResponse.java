package com.sep.vox.application.response.input.dashboard;

import java.util.UUID;

import com.sep.vox.application.projection.dto.AssessmentAwaitingPublishDto;

/** Kỳ thi đã đóng còn bài chưa có điểm — công bố là đóng vĩnh viễn mọi lối ra cho những bài đó. */
public record AssessmentAwaitingPublishResponse(
    UUID assessmentId,
    String code,
    String name,
    /** ISO-8601; null với kỳ không đặt mốc đóng. */
    String closeAt,
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
            dto.closeAt() == null ? null : dto.closeAt().toString(),
            dto.unscoredCount(),
            dto.aiFailedRetryLeft(),
            dto.aiFailedNoRetryLeft(),
            dto.awaitingHumanGrading()
        );
    }
}
