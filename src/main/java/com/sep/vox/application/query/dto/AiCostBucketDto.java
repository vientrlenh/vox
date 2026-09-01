package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Chi phí AI của một mốc thời gian, tách theo loại ví. */
public record AiCostBucketDto(
    Instant bucket,
    String quotaType,
    BigDecimal costVnd
) {
}
