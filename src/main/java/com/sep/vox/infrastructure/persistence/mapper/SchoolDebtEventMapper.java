package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolDebtEvent;
import com.sep.vox.domain.model.subscription.SchoolDebtEventType;
import com.sep.vox.infrastructure.persistence.entity.SchoolDebtEventJpaEntity;

public final class SchoolDebtEventMapper {

    private SchoolDebtEventMapper() {}

    public static SchoolDebtEvent toDomain(SchoolDebtEventJpaEntity jpa) {
        return new SchoolDebtEvent(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getSubscriptionId(),
            SchoolDebtEventType.valueOf(jpa.getEventType()),
            QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getTriggerExamSessionId(),
            jpa.getTriggerAmountUsd(),
            jpa.getTotalAllocatedUsd(),
            jpa.getUsedQuantityUsd(),
            jpa.getOverageUsd(),
            jpa.getOccurredAt()
        );
    }

    public static SchoolDebtEventJpaEntity toJpa(SchoolDebtEvent domain) {
        return new SchoolDebtEventJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getSubscriptionId(),
            domain.getEventType().name(),
            domain.getQuotaType().name(),
            domain.getTriggerExamSessionId(),
            domain.getTriggerAmountUsd(),
            domain.getTotalAllocatedUsd(),
            domain.getUsedQuantityUsd(),
            domain.getOverageUsd(),
            domain.getOccurredAt()
        );
    }
}
