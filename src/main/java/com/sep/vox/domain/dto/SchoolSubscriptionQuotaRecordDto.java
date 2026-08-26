package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;

/** Một ví hạn mức cấp TRƯỜNG của kỳ đăng ký hiện tại. */
public record SchoolSubscriptionQuotaRecordDto(
    UUID id,
    UUID schoolSubscriptionId,
    String quotaType,
    BigDecimal totalAllocatedAmountVnd,
    BigDecimal usedAmountVnd
) {

    public static SchoolSubscriptionQuotaRecordDto toDto(SchoolSubscriptionQuotaRecord domain) {
        return new SchoolSubscriptionQuotaRecordDto(
            domain.getId(),
            domain.getSchoolSubscriptionId(),
            valueOf(domain.getQuotaType()),
            domain.getTotalAllocatedAmountVnd(),
            domain.getUsedAmountVnd()
        );
    }

    public static List<SchoolSubscriptionQuotaRecordDto> toDtoList(List<SchoolSubscriptionQuotaRecord> domains) {
        return domains.stream().map(SchoolSubscriptionQuotaRecordDto::toDto).toList();
    }

    private static String valueOf(QuotaType type) {
        return type == null ? null : type.name();
    }
}
