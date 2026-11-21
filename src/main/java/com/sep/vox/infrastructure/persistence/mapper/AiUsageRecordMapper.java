package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.AiUsageRecord;
import com.sep.vox.domain.model.metering.AiUsageType;
import com.sep.vox.infrastructure.persistence.entity.metering.AiUsageRecordJpaEntity;

public final class AiUsageRecordMapper {

    private AiUsageRecordMapper() {}

    public static AiUsageRecord toDomain(AiUsageRecordJpaEntity jpa) {
        return AiUsageRecord.builder()
            .id(jpa.getId())
            .assessmentSessionId(jpa.getAssessmentSessionId())
            .turnId(jpa.getTurnId())
            .usageEventId(jpa.getUsageEventId())
            .usageType(fromString(jpa.getUsageType()))
            .provider(jpa.getProvider())
            .modelName(jpa.getModelName())
            .inputTokens(jpa.getInputTokens())
            .outputTokens(jpa.getOutputTokens())
            .cacheCreationInputTokens(jpa.getCacheCreationInputTokens())
            .cacheReadInputTokens(jpa.getCacheReadInputTokens())
            .durationMs(jpa.getDurationMs())
            .unitPriceJson(jpa.getUnitPriceJson())
            .costUsd(jpa.getCostUsd())
            .costVnd(jpa.getCostVnd())
            .fxRateUsed(jpa.getFxRateUsed())
            .occurredAt(jpa.getOccurredAt())
            .build();
    }

    public static AiUsageRecordJpaEntity toJpa(AiUsageRecord domain) {
        return AiUsageRecordJpaEntity.builder()
            .id(domain.getId())
            .assessmentSessionId(domain.getAssessmentSessionId())
            .turnId(domain.getTurnId())
            .usageEventId(domain.getUsageEventId())
            .usageType(valueOf(domain.getUsageType()))
            .provider(domain.getProvider())
            .modelName(domain.getModelName())
            .inputTokens(domain.getInputTokens())
            .outputTokens(domain.getOutputTokens())
            .cacheCreationInputTokens(domain.getCacheCreationInputTokens())
            .cacheReadInputTokens(domain.getCacheReadInputTokens())
            .durationMs(domain.getDurationMs())
            .unitPriceJson(domain.getUnitPriceJson())
            .costUsd(domain.getCostUsd())
            .costVnd(domain.getCostVnd())
            .fxRateUsed(domain.getFxRateUsed())
            .occurredAt(domain.getOccurredAt())
            .build();
    }

    private static AiUsageType fromString(String type) {
        if (type == null)
            return null;
        try {
            return AiUsageType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại sử dụng của AI model khi chuyển đổi sang domain model không hợp lệ: " + type);
        }
    }

    private static String valueOf(AiUsageType type) {
        return type == null ? null : type.name();
    }
}