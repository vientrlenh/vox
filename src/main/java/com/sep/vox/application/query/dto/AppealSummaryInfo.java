package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AppealSummaryInfo(
    UUID id,
    String studentName,
    String className,
    String examName,
    String partLabel,
    BigDecimal originalScore,
    String status,
    OffsetDateTime requestedAt,
    OffsetDateTime deadline,
    int reviewerCount,
    int doneCount
) {
}
