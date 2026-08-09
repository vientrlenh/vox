package com.sep.vox.domain.dto;

import java.util.List;

public record TokenUsageTimeseriesDto(
    String granularity,
    Integer totalUsed,
    List<TokenUsageTimeseriesPointDto> points,
    List<SubscriptionQuotaDto> currentPeriod
) {
}
