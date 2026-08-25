package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Nợ hạn mức AI của 1 trường vừa vượt trần cảnh báo (xem QuotaDebtProperties/ConsumeQuotaUseCase).
 * Người nhận là mọi SYSTEM_ADMIN -- CHỐT NGAY lúc phát sự kiện, không truy vấn lại ở consumer, cùng lý
 * do với InvoicePaidPayloadV1.systemAdminIds.
 */
public record SchoolDebtCapExceededPayloadV1(
    List<UUID> systemAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    QuotaType quotaType,
    BigDecimal overageUsd,
    BigDecimal capUsd,
    Instant occurredAt
) {
}
