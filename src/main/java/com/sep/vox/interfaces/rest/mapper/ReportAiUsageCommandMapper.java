package com.sep.vox.interfaces.rest.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.RecordAiUsageCommand;
import com.sep.vox.application.port.input.command.ReportAiUsageCommand;
import com.sep.vox.domain.model.metering.AiUsageType;
import com.sep.vox.interfaces.rest.dto.request.ReportAiUsageRequest;

import tools.jackson.databind.json.JsonMapper;

public final class ReportAiUsageCommandMapper {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private ReportAiUsageCommandMapper() {}

    public static ReportAiUsageCommand toCommand(UUID examSessionId, ReportAiUsageRequest request) {
        List<RecordAiUsageCommand> usageEvents = request.usageEvents().stream()
            .map(item -> toRecordCommand(examSessionId, request.turnId(), item))
            .toList();
        return new ReportAiUsageCommand(examSessionId, usageEvents);
    }

    private static RecordAiUsageCommand toRecordCommand(
            UUID examSessionId, UUID turnId, ReportAiUsageRequest.AiUsageEventItemRequest item) {
        return new RecordAiUsageCommand(
            examSessionId,
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
