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
 * Đường REST song song với Kafka topic ai-usage-recorded (AiUsageRecordedConsumer) -- dùng cho
 * nguồn không có kết nối Kafka trực tiếp, ví dụ desktop app tự tổng hợp TTS trên máy học viên
 * (VoxExamDesktop/LocalAvatarSpeaker.cs) thay vì qua Agentic AI. Cùng schema, cùng đổ vào
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
