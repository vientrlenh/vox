package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class ExamItemScore {
    private UUID id;
    private UUID responseId;
    private UUID paperItemId;
    private Map<String, BigDecimal> rubricScores;
    private BigDecimal itemScore;
    private String gradedByModel;
    private OffsetDateTime gradedAt;
    private ExamScoreStatus status;
}
