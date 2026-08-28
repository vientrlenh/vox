package com.sep.vox.domain.model.payment;

/**
 * Vòng đời của MỘT LẦN THỬ thanh toán, không phải của đơn hàng.
 *
 * <p>PENDING sinh ra ngay lúc tạo link (chưa ai trả), rồi chuyển sang PAID/FAILED khi cổng báo về
 * hoặc khi mình chủ động hỏi lại. Phải có PENDING thì mới giữ được provider_order_ref của lần thử
 * đang treo -- không có nó thì webhook không tra ngược được callback về đơn nào, và trước khi phát
 * link mới cũng không hỏi lại được lần thử cũ đã trả hay chưa (đường dẫn tới thu tiền hai lần).
 *
 * <p>CỐ Ý không tách EXPIRED/CANCELLED ở đây: với một lần thử thì chỉ có "ra tiền" hay "không ra
 * tiền". Sắc thái vì sao không ra tiền là chuyện của đơn hàng -- xem
 * {@link com.sep.vox.domain.model.order.OrderStatus} (CANCELLED/EXPIRED/FAILED).
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED
}
