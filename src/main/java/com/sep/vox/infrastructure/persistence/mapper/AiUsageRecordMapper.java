package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.AiUsageRecord;
import com.sep.vox.domain.model.metering.AiUsageType;
import com.sep.vox.infrastructure.persistence.entity.AiUsageRecordJpaEntity;

public final class AiUsageRecordMapper {

    private AiUsageRecordMapper() {}

    public static AiUsageRecord toDomain(AiUsageRecordJpaEntity jpa) {
        return new AiUsageRecord(
            jpa.getId(),
            jpa.getExamSessionId(),
            jpa.getTurnId(),
            jpa.getUsageEventId(),
            fromString(jpa.getUsageType()),
            jpa.getProvider(),
            jpa.getModelName(),
            jpa.getInputTokens(),
            jpa.getOutputTokens(),
            jpa.getCacheCreationInputTokens(),
            jpa.getCacheReadInputTokens(),
            jpa.getDurationMs(),
            jpa.getUnitPriceJson(),
            jpa.getCostUsd(), 
            jpa.getCostVnd(), 
            jpa.getFxRateUsed(), 
            jpa.getOccurredAt()
        );
    }

    public static AiUsageRecordJpaEntity toJpa(AiUsageRecord domain) {
        return new AiUsageRecordJpaEntity(
            domain.getId(),
            domain.getExamSessionId(),
            domain.getTurnId(),
            domain.getUsageEventId(),
            valueOf(domain.getUsageType()),
            domain.getProvider(),
            domain.getModelName(),
            domain.getInputTokens(),
            domain.getOutputTokens(),
            domain.getCacheCreationInputTokens(),
            domain.getCacheReadInputTokens(),
            domain.getDurationMs(),
            domain.getUnitPriceJson(),
            domain.getCostUsd(), 
            domain.getCostVnd(), 
            domain.getFxRateUsed(), 
            domain.getOccurredAt()
        );
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