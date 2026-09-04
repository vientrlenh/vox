package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;

/**
 * Một ví hạn mức cấp TRƯỜNG của kỳ đăng ký hiện tại.
 *
 * @param fundedFromBalanceVnd phần của {@code totalAllocatedAmountVnd} do trường tự nạp từ ví thay vì
 *                             do gói cấp (V12). Sau lần nạp đầu tiên thì total đã trộn hai nguồn, nên
 *                             đây là con số duy nhất trả lời được "gói cho bao nhiêu, mình bỏ thêm
 *                             bao nhiêu" -- và cũng là phần DUY NHẤT sống sót qua lần gia hạn kế tiếp.
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
