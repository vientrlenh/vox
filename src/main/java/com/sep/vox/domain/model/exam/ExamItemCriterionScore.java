package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamItemCriterionScore {
    private UUID id;
    private UUID evaluationId;
    private UUID rubricCriterionId;
    private BigDecimal rawScore;
    private BigDecimal finalScore;
    private String rationale;
}
