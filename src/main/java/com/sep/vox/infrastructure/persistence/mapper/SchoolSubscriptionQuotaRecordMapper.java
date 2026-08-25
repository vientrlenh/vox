package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionQuotaRecordJpaEntity;

public final class SchoolSubscriptionQuotaRecordMapper {

    private SchoolSubscriptionQuotaRecordMapper() {}

    public static SchoolSubscriptionQuotaRecord toDomain(SchoolSubscriptionQuotaRecordJpaEntity jpa) {
        return new SchoolSubscriptionQuotaRecord(
            jpa.getId(),
            jpa.getSubscriptionId(),
            QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getTotalAllocated(),
            jpa.getUsedQuantity()
        );
    }

    public static SchoolSubscriptionQuotaRecordJpaEntity toJpa(SchoolSubscriptionQuotaRecord domain) {
        return new SchoolSubscriptionQuotaRecordJpaEntity(
            domain.getId(),
            domain.getSubscriptionId(),
            domain.getQuotaType().name(),
            domain.getTotalAllocated(),
            domain.getUsedQuantity()
        );
    }
}
