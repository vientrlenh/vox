package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Ví hạn mức AI (EXAM hoặc PRACTICE) của 1 trường vừa CHUYỂN từ dưới sang vượt ngưỡng cảnh báo SỚM
 * (xem QuotaUsageWarningProperties/ConsumeQuotaService.checkUsageWarningTransition) -- hạn mức CHƯA
 * cạn, chỉ là báo trước để trường cân nhắc gia hạn/nạp thêm. Người nhận là mọi SCHOOL_ADMIN của
 * trường -- CHỐT NGAY lúc phát sự kiện, cùng lý do với SchoolLockedDueToDebtPayloadV1.schoolAdminIds.
 */
public record SchoolQuotaUsageWarningPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    QuotaType quotaType,
    BigDecimal totalAllocatedVnd,
    BigDecimal usedAmountVnd,
    Instant occurredAt
) {
}
