package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamSessionStatus;

public record ExamAttemptSummary(
    UUID candidateId,
    UUID examId,
    UUID sessionId,
    OffsetDateTime startedAt,
    OffsetDateTime submittedAt,
    ExamSessionStatus status,
    BigDecimal totalScore,
    UUID rubricResultBandId,
    String rubricResultBandCode,
    String rubricResultBandName,
    ExamCandidateResultStatus resultStatus
) {
    // Auxiliary constructor for JPQL "SELECT NEW ...ExamAttemptSummary(...)"
    // projections: status/resultStatus are plain String columns on the JPA
    // entities (enum conversion normally happens at the domain-mapper layer,
    // not on the entity), so this overload accepts the raw strings straight
    // out of the query and converts them here, keeping the record itself
    // properly enum-typed for every other caller.
    public ExamAttemptSummary(
            UUID candidateId,
            UUID examId,
            UUID sessionId,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            String status,
            BigDecimal totalScore,
            UUID rubricResultBandId,
            String rubricResultBandCode,
            String rubricResultBandName,
            String resultStatus) {
        this(
            candidateId,
            examId,
            sessionId,
            startedAt,
            submittedAt,
            ExamSessionStatus.valueOf(status),
            totalScore,
            rubricResultBandId,
            rubricResultBandCode,
            rubricResultBandName,
            resultStatus == null ? null : ExamCandidateResultStatus.valueOf(resultStatus)
        );
    }
}
