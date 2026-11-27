package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One row of the score sheet, for CSV export. */
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
    /** The latest completed grading round; empty when the submission has been through none. */
    String lastRoundType,
    String lastOutcome,
    String lastGraderName,
    Instant releasedAt
) {
}
