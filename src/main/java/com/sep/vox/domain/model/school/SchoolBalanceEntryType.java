package com.sep.vox.domain.model.school;

public enum SchoolBalanceEntryType {
    /** Trường nạp thêm tiền vào số dư -- luôn gắn với một Order đã thanh toán. */
    TOP_UP,
    /**
     * Trừ phần VƯỢT hạn mức kèm gói. Tiêu dùng còn nằm trong hạn mức đã được ghi ở
     * school_subscription_quota_records + ai_usage_records, KHÔNG lặp lại ở sổ cái này.
     */
    OVERAGE_CHARGE,
    /**
     * Trường chuyển tiền từ ví tự nạp sang ví HẠN MỨC của một loại quota (school_subscription_quota
     * _records.total_allocated_amount_vnd). Tiền rời ví nhưng KHÔNG phải một khoản chi cho AI -- nó
     * sang một túi khác của cùng nhà trường, nên không có cost_usd/fx_rate_used và không gắn phiên
     * nào.
     *
     * <p>Một CHIỀU: không có đường chuyển ngược về ví, cũng không chuyển được sang loại quota kia.
     * Nhầm ví thì chỉ System Admin gỡ được bằng một dòng ADJUSTMENT.
     *
     * <p>Bắt buộc có actor + quotaType -- xem chk_school_balance_entries_quota_funding_traceable (V12).
     */
    QUOTA_FUNDING,
    /** Hoàn tiền (vd trường trả trùng một đơn) -- luôn gắn với Order gốc. */
    REFUND,
    /** System Admin điều chỉnh tay, bắt buộc có actor + lý do. */
    ADJUSTMENT
}
