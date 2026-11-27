package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** The AI cost of one point in time, split by wallet type. */
public record AiCostBucketDto(
    Instant bucket,
    String quotaType,
    BigDecimal costVnd
) {
}
