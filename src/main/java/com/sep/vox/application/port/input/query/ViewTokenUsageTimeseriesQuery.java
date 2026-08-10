package com.sep.vox.application.port.input.query;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.TokenUsageGranularity;

public record ViewTokenUsageTimeseriesQuery(
    UUID schoolId,
    Instant dateFrom,
    Instant dateTo,
    TokenUsageGranularity granularity
) {
}
