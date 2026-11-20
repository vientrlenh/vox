package com.sep.vox.application.response.input.dashboard;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.query.dto.SchoolGradingFailureDto;

/** One row of the school's "AI grading failed, nobody has handled it yet" screen. */
public record SchoolGradingFailureResponse(
    UUID sessionId,
    UUID assessmentId,
    String assessmentCode,
    String assessmentName,
    String candidateName,
    String className,
    Instant failedAt,
    String error,
    Integer aiRetryCount,
    boolean schoolRetryLeft
) {

    public static SchoolGradingFailureResponse of(SchoolGradingFailureDto dto) {
        return new SchoolGradingFailureResponse(
            dto.sessionId(),
            dto.assessmentId(),
            dto.assessmentCode(),
            dto.assessmentName(),
            dto.candidateName(),
            dto.className(),
            dto.failedAt(),
            dto.error(),
            dto.aiRetryCount(),
            dto.schoolRetryLeft()
        );
    }
}
