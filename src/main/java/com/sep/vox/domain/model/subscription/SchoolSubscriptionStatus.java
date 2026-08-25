package com.sep.vox.domain.model.subscription;

public enum SchoolSubscriptionStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED,
    // System Admin cưỡng chế mất quyền dùng NGAY (khác CANCELLED chỉ tắt gia hạn, vẫn ACTIVE tới hết
    // endDate) -- xem ForceSuspendSubscriptionUseCase.
    SUSPENDED
}
