package com.sep.vox.application.response.input.examsession;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExamCandidateResultResponse(
    UUID id,
    UUID sessionId,
    UUID examId,
    UUID paperId,
    UUID candidateId,
    boolean flagged,
    String flagReason,
    boolean scoreVisible,
    BigDecimal totalScore,
    BigDecimal scoringScaleMin,
    BigDecimal scoringScaleMax,
    UUID targetFrameworkBandId,
    String targetFrameworkBandCode,
    String targetFrameworkBandLabel,
    UUID rubricResultBandId,
    String rubricResultBandCode,
    String rubricResultBandName,
    String status,
    List<ExamCandidateResultSectionResponse> sections,
    List<ExamCandidateResultItemResponse> items
) {
}
