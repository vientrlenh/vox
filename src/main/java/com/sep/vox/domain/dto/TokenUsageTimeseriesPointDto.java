package com.sep.vox.domain.dto;

import java.math.BigDecimal;

public record TokenUsageTimeseriesPointDto(
    String bucket,
    String quotaType,
    BigDecimal tokensConsumed
) {
}
