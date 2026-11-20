package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExamCandidateAttempts(
    List<ExamAttemptSummary> attempts,
    ExamAttemptSummary officialAttempt,
    BigDecimal officialScore
) {
}
