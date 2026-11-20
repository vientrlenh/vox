package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.infrastructure.persistence.entity.SchoolBalanceEntryJpaEntity;

public final class SchoolBalanceEntryMapper {

    private SchoolBalanceEntryMapper() {}

    public static SchoolBalanceEntry toDomain(SchoolBalanceEntryJpaEntity jpa) {
        return new SchoolBalanceEntry(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getSubscriptionId(),
            entryTypeFromString(jpa.getEntryType()),
            jpa.getAmountVnd(),
            jpa.getBalanceAfterVnd(),
            jpa.getOrderId(),
            jpa.getExamSessionId(),
            jpa.getPracticeSessionId(),
            quotaTypeFromString(jpa.getQuotaType()),
            jpa.getCostUsd(),
            jpa.getFxRateUsed(),
            jpa.getActorId(),
            jpa.getReason(),
            jpa.getOccurredAt()
        );
    }

    public static SchoolBalanceEntryJpaEntity toJpa(SchoolBalanceEntry domain) {
        return new SchoolBalanceEntryJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getSubscriptionId(),
            valueOf(domain.getEntryType()),
            domain.getAmountVnd(),
            domain.getBalanceAfterVnd(),
            domain.getOrderId(),
            domain.getExamSessionId(),
            domain.getPracticeSessionId(),
            valueOf(domain.getQuotaType()),
            domain.getCostUsd(),
            domain.getFxRateUsed(),
            domain.getActorId(),
            domain.getReason(),
            domain.getOccurredAt()
        );
    }

    private static SchoolBalanceEntryType entryTypeFromString(String entryType) {
        if (entryType == null)
            return null;
        try {
            return SchoolBalanceEntryType.valueOf(entryType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại bút toán số dư khi chuyển đổi sang domain model không hợp lệ: " + entryType);
        }
    }

    // quota_type nullable ở đây (chỉ OVERAGE_CHARGE bắt buộc có) nên null là hợp lệ, không phải lỗi.
    private static QuotaType quotaTypeFromString(String quotaType) {
        if (quotaType == null)
            return null;
        try {
            return QuotaType.valueOf(quotaType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại quota khi chuyển đổi sang domain model không hợp lệ: " + quotaType);
        }
    }

    private static String valueOf(SchoolBalanceEntryType entryType) {
        return entryType == null ? null : entryType.name();
    }

    private static String valueOf(QuotaType quotaType) {
        return quotaType == null ? null : quotaType.name();
    }
}
