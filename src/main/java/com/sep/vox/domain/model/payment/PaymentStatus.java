package com.sep.vox.domain.model.payment;

/**
 * Chỉ có hai kết cục vì PaymentRecord CHỈ được tạo khi cổng đã báo kết quả về (webhook). Các trạng
 * thái chờ/hết hạn/hủy nằm ở Order (PENDING/EXPIRED/CANCELLED) chứ không ở đây -- một đường dẫn
 * thanh toán chưa ai trả thì chưa sinh ra PaymentRecord nào cả.
 */
public enum PaymentStatus {
    PAID,
    FAILED
}
