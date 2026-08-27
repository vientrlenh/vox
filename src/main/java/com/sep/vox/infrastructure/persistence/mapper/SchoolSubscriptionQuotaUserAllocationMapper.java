package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionQuotaUserAllocationJpaEntity;

public final class SchoolSubscriptionQuotaUserAllocationMapper {

    private SchoolSubscriptionQuotaUserAllocationMapper() {}

    public static SchoolSubscriptionQuotaUserAllocation toDomain(SchoolSubscriptionQuotaUserAllocationJpaEntity jpa) {
        return new SchoolSubscriptionQuotaUserAllocation(
            jpa.getId(),
            jpa.getSchoolSubscriptionId(),
            fromString(jpa.getQuotaType()),
            jpa.getUserId(),
            jpa.getAllocatedAmountVnd(),
            jpa.getUsedAmountVnd()
        );
    }

    public static SchoolSubscriptionQuotaUserAllocationJpaEntity toJpa(SchoolSubscriptionQuotaUserAllocation domain) {
        return new SchoolSubscriptionQuotaUserAllocationJpaEntity(
            domain.getId(),
            domain.getSchoolSubscriptionId(),
            valueOf(domain.getQuotaType()),
            domain.getUserId(),
            domain.getAllocatedAmountVnd(),
            domain.getUsedAmountVnd()
        );
    }

    private static QuotaType fromString(String type) {
        if (type == null) 
            return null;
        try {
            return QuotaType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại quota không hợp lệ khi chuyển đổi sang domain model: " + type);
        }
    }

    private static String valueOf(QuotaType type) {
        return type == null ? null : type.name();
    }
}
