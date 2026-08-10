package com.sep.vox.domain.dto;

public record TokenUsageTimeseriesPointDto(
    String bucket,
    String quotaType,
    Integer tokensConsumed
) {
}
