package com.sep.vox.application.query.dto;

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
    /** A session is identified by its start time -- {@code assessment_schedules} has no name column. */
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
