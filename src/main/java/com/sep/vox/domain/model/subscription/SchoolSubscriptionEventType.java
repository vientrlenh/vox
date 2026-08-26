package com.sep.vox.domain.model.subscription;

public enum SchoolSubscriptionEventType {
    /** System Admin cưỡng chế mất quyền dùng NGAY -- luôn kèm lý do. */
    SUSPENDED,
    /** Gỡ đình chỉ, trả gói về ACTIVE. */
    UNSUSPENDED
}
