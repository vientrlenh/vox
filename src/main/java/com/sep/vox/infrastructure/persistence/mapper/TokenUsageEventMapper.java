package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.TokenUsageEvent;
import com.sep.vox.infrastructure.persistence.entity.TokenUsageEventJpaEntity;

public final class TokenUsageEventMapper {

    private TokenUsageEventMapper() {}

    public static TokenUsageEvent toDomain(TokenUsageEventJpaEntity jpa) {
        return new TokenUsageEvent(
            jpa.getId(),
            jpa.getSubscriptionId(),
            jpa.getExamSessionId(),
            QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getTokensConsumed(),
            jpa.getOccurredAt()
        );
    }

    public static TokenUsageEventJpaEntity toJpa(TokenUsageEvent domain) {
        return new TokenUsageEventJpaEntity(
            domain.getId(),
            domain.getSubscriptionId(),
            domain.getExamSessionId(),
            domain.getQuotaType().name(),
            domain.getTokensConsumed(),
            domain.getOccurredAt()
        );
    }
}
