package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.aimodel.AiUsageType;

public record RecordAiUsageCommand(
    UUID examSessionId,
    UUID turnId,
    UUID usageEventId,
    AiUsageType usageType,
    String provider,
    String modelName,
    Integer inputTokens,
    Integer outputTokens,
    Integer cacheCreationInputTokens,
    Integer cacheReadInputTokens,
    Long durationMs,
    String unitPriceJson,
    BigDecimal costUsd,
    Instant occurredAt
) {
}