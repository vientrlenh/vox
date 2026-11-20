package com.sep.vox.application.response.input.dashboard;

import java.util.UUID;

import com.sep.vox.application.query.dto.SchoolGradingFailureDto;

/** Một dòng trên màn "AI chấm lỗi, chưa ai xử lý" của nhà trường. */
public record SchoolGradingFailureResponse(
    UUID sessionId,
    UUID examId,
    String examCode,
    String examName,
    String candidateName,
    String className,
    /** ISO-8601. */
    String failedAt,
    String error,
    Integer aiRetryCount,
    boolean schoolRetryLeft
) {

    public static SchoolGradingFailureResponse of(SchoolGradingFailureDto dto) {
        return new SchoolGradingFailureResponse(
            dto.sessionId(),
            dto.examId(),
            dto.examCode(),
            dto.examName(),
            dto.candidateName(),
            dto.className(),
            dto.failedAt() == null ? null : dto.failedAt().toString(),
            dto.error(),
            dto.aiRetryCount(),
            dto.schoolRetryLeft()
        );
    }
}
