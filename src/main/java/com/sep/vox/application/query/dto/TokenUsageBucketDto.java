package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TokenUsageBucketDto(
    Instant bucket,
    String quotaType,
    BigDecimal tokensConsumed
) {
}
