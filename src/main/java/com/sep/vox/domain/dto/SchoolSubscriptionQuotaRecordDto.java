package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;

/**
 * A SCHOOL-level quota wallet for the current subscription period.
 *
 * @param fundedFromBalanceVnd the part of {@code totalAllocatedAmountVnd} that the school topped up from its balance
 *                             instead of receiving from the plan (V12). After the first top-up the total mixes
 *                             two sources, so this is the only number that answers "how much did the plan give and
 *                             how much did we add" -- and it is also the ONLY part that survives the next renewal.
 */
public record SchoolSubscriptionQuotaRecordDto(
    UUID id,
    UUID schoolSubscriptionId,
    String quotaType,
    BigDecimal totalAllocatedAmountVnd,
    BigDecimal usedAmountVnd,
    BigDecimal fundedFromBalanceVnd
) {

    public static SchoolSubscriptionQuotaRecordDto toDto(SchoolSubscriptionQuotaRecord domain) {
        return new SchoolSubscriptionQuotaRecordDto(
            domain.getId(),
            domain.getSchoolSubscriptionId(),
            valueOf(domain.getQuotaType()),
            domain.getTotalAllocatedAmountVnd(),
            domain.getUsedAmountVnd(),
            domain.getFundedFromBalanceVnd()
        );
    }

    public static List<SchoolSubscriptionQuotaRecordDto> toDtoList(List<SchoolSubscriptionQuotaRecord> domains) {
        return domains.stream().map(SchoolSubscriptionQuotaRecordDto::toDto).toList();
    }

    private static String valueOf(QuotaType type) {
        return type == null ? null : type.name();
    }
}
