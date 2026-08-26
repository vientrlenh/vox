package com.sep.vox.application.response.output;

/**
 * Cách FE phải mở trang thanh toán của cổng. Đây là thứ trước đây không diễn đạt được: trường
 * {@code method} cũ mang tên cổng ("PAYOS"/"SEPAY"), nên FE buộc phải hardcode "nếu là SEPAY thì
 * submit form" — tức là mỗi lần thêm cổng mới lại phải sửa FE.
 */
public enum CheckoutAction {

    /** Điều hướng trình duyệt sang {@code actionUrl} (PayOS trả về link checkout riêng cho từng đơn). */
    REDIRECT,

    /**
     * Dựng form ẩn với toàn bộ {@code fields} rồi POST sang {@code actionUrl}. SePay PG dùng cách
     * này: URL checkout là cố định, đơn hàng được mô tả bằng các field kèm chữ ký HMAC.
     */
    FORM_POST,

    /**
     * Không cần thanh toán gì thêm — hóa đơn đã được chốt PAID ngay (amountDue = 0 sau khi bù trừ
     * ngày chưa dùng, xem SubscriptionUpgradePolicyService), FE không cần điều hướng sang cổng nào.
     */
    NONE
}
