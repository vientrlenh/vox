package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.AiUsageRecord;
import com.sep.vox.domain.model.subscription.AiUsageType;
import com.sep.vox.infrastructure.persistence.entity.AiUsageRecordJpaEntity;

public final class AiUsageRecordMapper {

    private AiUsageRecordMapper() {}

    public static AiUsageRecord toDomain(AiUsageRecordJpaEntity jpa) {
        return new AiUsageRecord(
            jpa.getId(),
            jpa.getExamSessionId(),
            jpa.getTurnId(),
            jpa.getUsageEventId(),
            AiUsageType.valueOf(jpa.getUsageType()),
            jpa.getProvider(),
            jpa.getModelName(),
            jpa.getInputTokens(),
            jpa.getOutputTokens(),
            jpa.getCacheCreationInputTokens(),
            jpa.getCacheReadInputTokens(),
            jpa.getDurationMs(),
            jpa.getUnitPriceJson(),
            jpa.getCostUsd(),
            jpa.getOccurredAt()
        );
    }

    public static AiUsageRecordJpaEntity toJpa(AiUsageRecord domain) {
        return new AiUsageRecordJpaEntity(
            domain.getId(),
            domain.getExamSessionId(),
            domain.getTurnId(),
            domain.getUsageEventId(),
            domain.getUsageType().name(),
            domain.getProvider(),
            domain.getModelName(),
            domain.getInputTokens(),
            domain.getOutputTokens(),
            domain.getCacheCreationInputTokens(),
            domain.getCacheReadInputTokens(),
            domain.getDurationMs(),
            domain.getUnitPriceJson(),
            domain.getCostUsd(),
            domain.getOccurredAt()
        );
    }
}