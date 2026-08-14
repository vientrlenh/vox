package com.sep.vox.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Trường vừa CHUYỂN từ bị khóa sang hết nợ (mua thêm token đủ để usedQuantity trở lại trong hạn mức).
 * Người nhận là mọi SCHOOL_ADMIN của trường -- CHỐT NGAY lúc phát sự kiện, cùng lý do với
 * InvoicePaidPayloadV1.schoolAdminIds.
 */
public record SchoolDebtClearedPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    Instant occurredAt
) {
}
