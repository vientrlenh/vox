package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.RecordAiUsageCommand;
import com.sep.vox.application.port.input.command.ReportAiUsageCommand;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.metering.AiUsageType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đường REST song song với Kafka topic ai-usage-recorded (AiUsageRecordedConsumer) -- dùng cho
 * nguồn không có kết nối Kafka trực tiếp, ví dụ desktop app tự tổng hợp TTS trên máy học viên
 * (VoxAssessmentDesktop/LocalAvatarSpeaker.cs) thay vì qua Agentic AI. Cùng schema, cùng đổ vào
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

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    public static ReportAiUsageCommand toCommand(UUID assessmentSessionId, ReportAiUsageRequest request) {
        List<RecordAiUsageCommand> usageEvents = request.usageEvents().stream()
            .map(item -> toRecordCommand(assessmentSessionId, request.turnId(), item))
            .toList();
        return new ReportAiUsageCommand(assessmentSessionId, usageEvents);
    }

    private static RecordAiUsageCommand toRecordCommand(
            UUID assessmentSessionId, UUID turnId, ReportAiUsageRequest.AiUsageEventItemRequest item) {
        return new RecordAiUsageCommand(
            assessmentSessionId,
            turnId,
            item.usageEventId(),
            parseUsageType(item.type()),
            item.provider(),
            item.model(),
            item.inputTokens(),
            item.outputTokens(),
            item.cacheCreationInputTokens(),
            item.cacheReadInputTokens(),
            item.durationMs(),
            item.unitPrice() == null ? "{}" : JSON_MAPPER.writeValueAsString(item.unitPrice()),
            item.costUsd(),
            item.occurredAt() == null ? Instant.now() : item.occurredAt()
        );
    }

    private static AiUsageType parseUsageType(String value) {
        if (value == null) {
            throw new IllegalArgumentException("type không được để trống");
        }
        try {
            return AiUsageType.valueOf(StringNormalization.normalizeCode(value));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("type không hợp lệ: " + value);
        }
    }
}
