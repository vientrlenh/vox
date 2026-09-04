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
     * <p>Một CHIỀU và HIỆN CHƯA gỡ được: không có đường chuyển ngược về ví, không chuyển được sang
     * loại quota kia, và một dòng ADJUSTMENT KHÔNG phải cách sửa -- nó cộng tiền lại vào ví nhưng
     * không hạ được ví hạn mức (mọi lệnh ghi school_subscription_quota_records đều chỉ cộng), nên
     * trường sẽ giữ cả hai. Xem FundQuotaFromBalanceUseCase.
     *
     * <p>Bắt buộc có actor + quotaType -- xem chk_school_balance_entries_quota_funding_traceable (V12).
     */
    QUOTA_FUNDING,
    /** Hoàn tiền (vd trường trả trùng một đơn) -- luôn gắn với Order gốc. */
    REFUND,
    /** System Admin điều chỉnh tay, bắt buộc có actor + lý do. */
    ADJUSTMENT
}
