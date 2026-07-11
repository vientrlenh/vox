package com.sep.vox.application.response.input.examsession;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamCandidateResultItemResponse(
    UUID paperItemId,
    UUID responseId,
    UUID sectionId,
    BigDecimal itemScore,
    BigDecimal weightedScore
) {
}
