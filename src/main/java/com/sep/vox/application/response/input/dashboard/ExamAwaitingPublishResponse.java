package com.sep.vox.application.response.input.dashboard;

import java.util.UUID;

import com.sep.vox.application.query.dto.ExamAwaitingPublishDto;

/** Kỳ thi đã đóng còn bài chưa có điểm — công bố là đóng vĩnh viễn mọi lối ra cho những bài đó. */
public record ExamAwaitingPublishResponse(
    UUID examId,
    String code,
    String name,
    /** ISO-8601; null với kỳ không đặt mốc đóng. */
    String closeAt,
    int unscoredCount,
    int aiFailedRetryLeft,
    int aiFailedNoRetryLeft,
    int awaitingHumanGrading
) {

    public static ExamAwaitingPublishResponse of(ExamAwaitingPublishDto dto) {
        return new ExamAwaitingPublishResponse(
            dto.examId(),
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
