package com.sep.vox.interfaces.kafka.dto;

import java.math.BigDecimal;
import java.util.List;

import tools.jackson.databind.JsonNode;

public record AiUsageRecordedEventDto(
    String eventType,
    String examSessionId,
    String turnId,
    List<AiUsageEventItemDto> usageEvents
) {
    // type = "LLM_TOKEN" | "DURATION" (khớp tên AiUsageType). provider dùng chung cho cả hai loại
    // (vd "anthropic"/"openai" cho LLM_TOKEN, "azure_stt"/"elevenlabs_tts" cho DURATION) -- không tách
    // riêng field "service" để tránh 2 field loại trừ lẫn nhau.
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