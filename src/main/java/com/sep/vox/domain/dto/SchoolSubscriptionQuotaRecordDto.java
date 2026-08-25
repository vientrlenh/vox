package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;

public record SchoolSubscriptionQuotaRecordDto(
    UUID id,
    UUID schoolSubscriptionId,
    String quotaType,
    BigDecimal totalAllocatedAmountVnd,
    BigDecimal usedAmountVnd,
    // usedQuantity > totalAllocated -- với GRADING/CLASS_TEST, đây chính là điều kiện khóa cấp
    // trường (xem SchoolSubscriptionDebtGuardService). Với PRACTICE chỉ mang tính thông tin, không
    // khóa gì (luồng PRACTICE vẫn chặn cứng ở ConsumeQuotaUseCase, không cho phép đi vào trạng thái này).
    boolean isLocked
) {

    public static SchoolSubscriptionQuotaRecordDto toDto(SchoolSubscriptionQuotaRecord domain) {
        return new SchoolSubscriptionQuotaRecordDto(
            domain.getId(),
            domain.getSchoolSubscriptionId(),
            valueOf(domain.getQuotaType()),
            domain.getTotalAllocatedAmountVnd(),
            domain.getUsedAmountVnd(),
            isLocked(domain)
        );
    }

    public static List<SchoolSubscriptionQuotaRecordDto> toDtoList(List<SchoolSubscriptionQuotaRecord> domains) {
        return domains.stream().map(SchoolSubscriptionQuotaRecordDto::toDto).toList();
    }

    private static boolean isLocked(SchoolSubscriptionQuotaRecord domain) {
        return domain.getUsedAmountVnd().compareTo(domain.getTotalAllocatedAmountVnd()) > 0;
    }

    private static String valueOf(QuotaType type) {
        return type == null ? null : type.name();
    }
}
