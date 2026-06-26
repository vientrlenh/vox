package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamResultAppeal {
    private UUID id;
    private UUID candidateResultId;
    private UUID requestedBy;
    private String reason;
    private OffsetDateTime requestedAt;

    private ExamAppealStatus status;
    private UUID resolutionEvaluationId; // lượt chấm lại
    private BigDecimal scoreBefore;
    private BigDecimal scoreAfter;
    private UUID resolvedBy;
    private OffsetDateTime resolvedAt;
    private String note;
    
}
