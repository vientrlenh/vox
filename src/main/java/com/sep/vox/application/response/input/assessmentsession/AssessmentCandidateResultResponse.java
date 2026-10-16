package com.sep.vox.application.response.input.assessmentsession;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AssessmentCandidateResultResponse(
    UUID id,
    UUID sessionId,
    UUID assessmentId,
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
    List<AssessmentCandidateResultSectionResponse> sections,
    List<AssessmentCandidateResultItemResponse> items
) {
}
