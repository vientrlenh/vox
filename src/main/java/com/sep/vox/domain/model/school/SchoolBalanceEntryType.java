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
    ADJUSTMENT
}
