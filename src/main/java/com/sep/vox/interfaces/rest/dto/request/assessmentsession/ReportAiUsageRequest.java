package com.sep.vox.interfaces.rest.dto.request.assessmentsession;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.assessmentsession.ReportAiUsageCommand;
import com.sep.vox.application.port.input.command.metering.RecordAiUsageCommand;
import com.sep.vox.domain.model.metering.AiUsageType;
import tools.jackson.databind.json.JsonMapper;

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
        @NotNull AiUsageType type,
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
            item.type(),
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
}
