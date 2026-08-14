package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record TokenUsageTimeseriesDto(
    String granularity,
    BigDecimal totalUsed,
    List<TokenUsageTimeseriesPointDto> points,
    List<SubscriptionQuotaDto> currentPeriod
) {
}
