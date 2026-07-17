package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.infrastructure.persistence.entity.FinancialEventJpaEntity;

public final class FinancialEventMapper {

    private FinancialEventMapper() {}

    public static FinancialEvent toDomain(FinancialEventJpaEntity jpa) {
        return new FinancialEvent(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getSubscriptionId(),
            FinancialEventType.valueOf(jpa.getEventType()),
            jpa.getAmountSigned(),
            jpa.getCurrency(),
            jpa.getActorId(),
            jpa.getPayload(),
            jpa.getOccurredAt()
        );
    }

    public static FinancialEventJpaEntity toJpa(FinancialEvent domain) {
        return new FinancialEventJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getSubscriptionId(),
            domain.getEventType().name(),
            domain.getAmountSigned(),
            domain.getCurrency(),
            domain.getActorId(),
            domain.getPayload(),
            domain.getOccurredAt()
        );
    }
}
