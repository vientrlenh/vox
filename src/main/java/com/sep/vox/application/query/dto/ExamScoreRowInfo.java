package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Một dòng bảng điểm để xuất CSV. */
public record ExamScoreRowInfo(
    UUID candidateResultId,
    String studentName,
    String studentEmail,
    String className,
    String examName,
    /** Ca thi được nhận diện bằng mốc bắt đầu — {@code exam_schedules} không có cột tên. */
    OffsetDateTime scheduleStartAt,
    BigDecimal totalScore,
    String resultBand,
    String status,
    /** Vòng chấm gần nhất đã hoàn thành; rỗng khi bài chưa qua vòng nào. */
    String lastRoundType,
    String lastOutcome,
    String lastGraderName,
    OffsetDateTime releasedAt
) {
}
