package com.sep.vox.application.query.dto;

import java.time.Instant;

public record TokenUsageBucketDto(
    Instant bucket,
    String quotaType,
    Integer tokensConsumed
) {
}
