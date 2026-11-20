package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * A REST path parallel to the Kafka topic ai-usage-recorded (AiUsageRecordedConsumer) -- for
 * sources with no direct Kafka connection, for example the desktop app that aggregates TTS itself on the learner's machine
 * (the desktop app, LocalAvatarSpeaker.cs) instead of going through the Agentic AI. The same schema, feeding the same
 * RecordAiUsageUseCase.
 */
public record ReportAiUsageRequest(
    @NotNull UUID turnId,
    @NotEmpty @Valid List<AiUsageEventItemRequest> usageEvents
) {
    public record AiUsageEventItemRequest(
        @NotNull UUID usageEventId,
        @NotNull String type,
        @NotNull String provider,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Integer cacheCreationInputTokens,
        Integer cacheReadInputTokens,
        Long durationMs,
        Map<String, Object> unitPrice,
        @NotNull BigDecimal costUsd,
        Instant occurredAt
    ) {
    }
}
