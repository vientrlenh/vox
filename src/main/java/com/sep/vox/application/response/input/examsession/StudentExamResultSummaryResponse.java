package com.sep.vox.application.response.input.examsession;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentExamResultSummaryResponse(
    UUID candidateId,
    UUID examId,
    String examCode,
    String examName,
    UUID sessionId,
    UUID paperId,
    String sessionStatus,
    String startedAt,
    String submittedAt,
    BigDecimal totalScore,
    String resultStatus,
    UUID rubricResultBandId,
    String rubricResultBandCode,
    String rubricResultBandName
) {
}
