package com.sep.vox.interfaces.kafka.dto;

import java.math.BigDecimal;
import java.util.List;

import tools.jackson.databind.JsonNode;

public record AiUsageRecordedEventDto(
    String eventType,
    String assessmentSessionId,
    String turnId,
    List<AiUsageEventItemDto> usageEvents
) {
    // type = "LLM_TOKEN" | "DURATION" (matches the AiUsageType names). provider is shared by both kinds
    // (e.g. "anthropic"/"openai" for LLM_TOKEN, "azure_stt"/"elevenlabs_tts" for DURATION) -- no separate
    // "service" field, to avoid 2 fields that exclude each other.
    public record AiUsageEventItemDto(
        String usageEventId,
        String type,
        String provider,
        String model,
        AiUsageTokensDto usage,
        Long durationMs,
        JsonNode unitPrice,
        BigDecimal costUsd,
        String occurredAt
    ) {
    }

    public record AiUsageTokensDto(
        Integer inputTokens,
        Integer outputTokens,
        Integer cacheCreationInputTokens,
        Integer cacheReadInputTokens
    ) {
    }
}