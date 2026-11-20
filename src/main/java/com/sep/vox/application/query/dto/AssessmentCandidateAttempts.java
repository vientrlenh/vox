package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;

public record AssessmentCandidateAttempts(
    List<AssessmentAttemptSummary> attempts,
    AssessmentAttemptSummary officialAttempt,
    BigDecimal officialScore
) {
}
