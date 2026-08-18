package com.sep.vox.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Trường vừa bị System Admin cưỡng chế đình chỉ gói (mất quyền dùng NGAY, khác hủy thường). Người nhận
 * là mọi SCHOOL_ADMIN của trường -- CHỐT NGAY lúc phát sự kiện, cùng lý do với
 * SchoolLockedDueToDebtPayloadV1.schoolAdminIds.
 */
public record SchoolSubscriptionSuspendedPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    String reason,
    Instant occurredAt
) {
}
