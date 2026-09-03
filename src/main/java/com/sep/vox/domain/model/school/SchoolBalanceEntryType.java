package com.sep.vox.domain.model.school;

public enum SchoolBalanceEntryType {
    /** Trường nạp thêm tiền vào số dư -- luôn gắn với một Order đã thanh toán. */
    TOP_UP,
    /**
     * Trừ phần VƯỢT hạn mức kèm gói. Tiêu dùng còn nằm trong hạn mức đã được ghi ở
     * school_subscription_quota_records + ai_usage_records, KHÔNG lặp lại ở sổ cái này.
     */
    OVERAGE_CHARGE,
    /** Hoàn tiền (vd trường trả trùng một đơn) -- luôn gắn với Order gốc. */
    REFUND,
    /** System Admin điều chỉnh tay, bắt buộc có actor + lý do. */
    ADJUSTMENT,
    /**
     * Trích/hoàn ví tự nạp do SCHOOL_ADMIN cấp hạn mức cá nhân (SchoolSubscriptionQuotaUserAllocation)
     * vượt phần pool của gói. Âm = cấp hạn mức, ăn vào ví. Dương = hạ hạn mức, hoàn lại phần đã ăn.
     * Bắt buộc có quotaType, targetUserId (người được cấp/hạ) và actorId (người xác nhận).
     */
    ALLOCATION_DRAW
}
