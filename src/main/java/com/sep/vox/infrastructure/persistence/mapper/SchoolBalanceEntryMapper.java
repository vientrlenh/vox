package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolBalanceEntryJpaEntity;

public final class SchoolBalanceEntryMapper {

    private SchoolBalanceEntryMapper() {}

    public static SchoolBalanceEntry toDomain(SchoolBalanceEntryJpaEntity jpa) {
        return SchoolBalanceEntry.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .subscriptionId(jpa.getSubscriptionId())
            .entryType(entryTypeFromString(jpa.getEntryType()))
            .amountVnd(jpa.getAmountVnd())
            .balanceAfterVnd(jpa.getBalanceAfterVnd())
            .orderId(jpa.getOrderId())
            .assessmentSessionId(jpa.getAssessmentSessionId())
            .practiceSessionId(jpa.getPracticeSessionId())
            .quotaType(quotaTypeFromString(jpa.getQuotaType()))
            .costUsd(jpa.getCostUsd())
            .fxRateUsed(jpa.getFxRateUsed())
            .actorId(jpa.getActorId())
            .reason(jpa.getReason())
            .occurredAt(jpa.getOccurredAt())
            .build();
    }

    public static SchoolBalanceEntryJpaEntity toJpa(SchoolBalanceEntry domain) {
        return SchoolBalanceEntryJpaEntity.builder()
            .id(domain.getId())
            .schoolId(domain.getSchoolId())
            .subscriptionId(domain.getSubscriptionId())
            .entryType(valueOf(domain.getEntryType()))
            .amountVnd(domain.getAmountVnd())
            .balanceAfterVnd(domain.getBalanceAfterVnd())
            .orderId(domain.getOrderId())
            .assessmentSessionId(domain.getAssessmentSessionId())
            .practiceSessionId(domain.getPracticeSessionId())
            .quotaType(valueOf(domain.getQuotaType()))
            .costUsd(domain.getCostUsd())
            .fxRateUsed(domain.getFxRateUsed())
            .actorId(domain.getActorId())
            .reason(domain.getReason())
            .occurredAt(domain.getOccurredAt())
            .build();
    }

    private static SchoolBalanceEntryType entryTypeFromString(String entryType) {
        if (entryType == null)
            return null;
        try {
            return SchoolBalanceEntryType.valueOf(entryType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid balance entry type when converting to the domain model: " + entryType);
        }
    }

    // quota_type is nullable here (only OVERAGE_CHARGE requires it), so null is valid, not an error.
    private static QuotaType quotaTypeFromString(String quotaType) {
        if (quotaType == null)
            return null;
        try {
            return QuotaType.valueOf(quotaType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid quota type when converting to the domain model: " + quotaType);
        }
    }

    private static String valueOf(SchoolBalanceEntryType entryType) {
        return entryType == null ? null : entryType.name();
    }

    private static String valueOf(QuotaType quotaType) {
        return quotaType == null ? null : quotaType.name();
    }
}
