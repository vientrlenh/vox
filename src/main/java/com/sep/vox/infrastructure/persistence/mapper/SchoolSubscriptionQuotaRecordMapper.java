package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionQuotaRecordJpaEntity;

public final class SchoolSubscriptionQuotaRecordMapper {

    private SchoolSubscriptionQuotaRecordMapper() {}

    public static SchoolSubscriptionQuotaRecord toDomain(SchoolSubscriptionQuotaRecordJpaEntity jpa) {
        var domain = new SchoolSubscriptionQuotaRecord(
            jpa.getId(),
            jpa.getSchoolSubscriptionId(),
            fromString(jpa.getQuotaType()),
            jpa.getTotalAllocatedAmountVnd(),
            jpa.getUsedAmountVnd(),
            jpa.getFundedFromBalanceVnd()
        );
        domain.setCarryFundingFromSubscriptionId(jpa.getCarryFundingFromSubscriptionId());
        return domain;
    }

    public static SchoolSubscriptionQuotaRecordJpaEntity toJpa(SchoolSubscriptionQuotaRecord domain) {
        return new SchoolSubscriptionQuotaRecordJpaEntity(
            domain.getId(),
            domain.getSchoolSubscriptionId(),
            valueOf(domain.getQuotaType()),
            domain.getTotalAllocatedAmountVnd(),
            domain.getUsedAmountVnd(),
            domain.getFundedFromBalanceVnd(),
            domain.getCarryFundingFromSubscriptionId()
        );
    }


    private static QuotaType fromString(String type) {
        if (type == null) 
            return null;
        try {
            return QuotaType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại quota khi chuyển đổi sang model không hợp lệ: " + type);
        }
    }


    private static String valueOf(QuotaType type) {
        return type == null ? null : type.name();
    }
}
