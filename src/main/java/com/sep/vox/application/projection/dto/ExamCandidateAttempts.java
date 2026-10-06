package com.sep.vox.application.projection.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExamCandidateAttempts(
    List<ExamAttemptSummary> attempts,
    ExamAttemptSummary officialAttempt,
    BigDecimal officialScore
) {
}
