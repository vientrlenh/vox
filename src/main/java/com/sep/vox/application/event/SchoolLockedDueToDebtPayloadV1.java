package com.sep.vox.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Trường vừa CHUYỂN sang trạng thái bị khóa vì nợ hạn mức AI (xem SchoolSubscriptionDebtGuardService).
 * Người nhận là mọi SCHOOL_ADMIN của trường -- CHỐT NGAY lúc phát sự kiện, cùng lý do với
 * InvoicePaidPayloadV1.schoolAdminIds.
 */
public record SchoolLockedDueToDebtPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    Instant occurredAt
) {
}
