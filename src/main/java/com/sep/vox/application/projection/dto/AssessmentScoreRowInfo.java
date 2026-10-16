package com.sep.vox.application.projection.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Một dòng bảng điểm để xuất CSV. */
public record AssessmentScoreRowInfo(
    UUID candidateResultId,
    String studentName,
    String studentEmail,
    String className,
    String assessmentName,
    /** Ca thi được nhận diện bằng mốc bắt đầu — {@code assessment_schedules} không có cột tên. */
    Instant scheduleStartAt,
    BigDecimal totalScore,
    String resultBand,
    String status,
    /** Vòng chấm gần nhất đã hoàn thành; rỗng khi bài chưa qua vòng nào. */
    String lastRoundType,
    String lastOutcome,
    String lastGraderName,
    Instant releasedAt
) {
}
