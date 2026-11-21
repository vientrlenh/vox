package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.infrastructure.persistence.entity.subscription.SchoolSubscriptionQuotaRecordJpaEntity;

public final class SchoolSubscriptionQuotaRecordMapper {

    private SchoolSubscriptionQuotaRecordMapper() {}

    public static SchoolSubscriptionQuotaRecord toDomain(SchoolSubscriptionQuotaRecordJpaEntity jpa) {
        return SchoolSubscriptionQuotaRecord.builder()
            .id(jpa.getId())
            .schoolSubscriptionId(jpa.getSchoolSubscriptionId())
            .quotaType(fromString(jpa.getQuotaType()))
            .totalAllocatedAmountVnd(jpa.getTotalAllocatedAmountVnd())
            .usedAmountVnd(jpa.getUsedAmountVnd())
            .fundedFromBalanceVnd(jpa.getFundedFromBalanceVnd())
            .carryFundingFromSubscriptionId(jpa.getCarryFundingFromSubscriptionId())
            .build();
    }

    public static SchoolSubscriptionQuotaRecordJpaEntity toJpa(SchoolSubscriptionQuotaRecord domain) {
        return SchoolSubscriptionQuotaRecordJpaEntity.builder()
            .id(domain.getId())
            .schoolSubscriptionId(domain.getSchoolSubscriptionId())
            .quotaType(valueOf(domain.getQuotaType()))
            .totalAllocatedAmountVnd(domain.getTotalAllocatedAmountVnd())
            .usedAmountVnd(domain.getUsedAmountVnd())
            .fundedFromBalanceVnd(domain.getFundedFromBalanceVnd())
            .carryFundingFromSubscriptionId(domain.getCarryFundingFromSubscriptionId())
            .build();
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
