package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.TokenUsageBucketDto;

public interface TokenUsageTimeseriesQueryRepository {
    /**
     * @param granularityUnit tham số truyền thẳng cho {@code date_trunc} của Postgres —
     *                         {@code "day"}, {@code "week"} hoặc {@code "month"} (chữ thường).
     */
    List<TokenUsageBucketDto> findBucketedUsage(UUID subscriptionId, Instant from, Instant to, String granularityUnit);
}
