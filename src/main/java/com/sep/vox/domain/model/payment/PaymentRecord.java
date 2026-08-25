package com.sep.vox.domain.model.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * MỘT LẦN THỬ thanh toán của một Order -- sinh ra ngay lúc phát link (PENDING), rồi chuyển sang
 * PAID/FAILED khi cổng báo về hoặc khi mình chủ động hỏi lại.
 *
 * <p>Một Order có NHIỀU PaymentRecord (trả hụt rồi trả lại), nhưng tối đa MỘT dòng PAID
 * (uq_payment_records_one_paid_per_order) và tối đa MỘT dòng PENDING
 * (uq_payment_records_one_pending_per_order). Ràng buộc PENDING là thứ ép đúng thứ tự: muốn phát
 * link mới thì phải chốt lần thử cũ trước, tức là phải hỏi cổng xem nó đã trả tiền chưa -- không có
 * nó thì trường bấm "thanh toán lại" nhiều lần và trả tiền hai lần cho một đơn.
 *
 * <p>{@code providerOrderRef} là KHÓA ĐỐI SOÁT với cổng và KHÔNG BAO GIỜ dùng lại: cả hai cổng đều
 * bắt mã đơn duy nhất phía họ (PayOS trả lỗi "Đơn thanh toán đã tồn tại"; SePay yêu cầu
 * {@code order_invoice_number} không trùng), nên mỗi lần thử phải mang một mã mới.
 *
 * <p>CỐ Ý không lưu checkoutUrl lẫn paymentLinkId, và cũng KHÔNG có cột payload/metadata dạng JSON
 * cho dữ liệu riêng của từng cổng. Mọi thao tác với cổng đều chỉ cần {@code providerOrderRef}:
 * PayOS tra theo orderCode, SePay tra theo order_invoice_number, và dashboard của cả hai đều tìm
 * được bằng chính mã đó. Một cột JSON tự do ở bảng tiền là đúng thứ V2 vừa bỏ đi khi thay cặp
 * (source_type, source_id) đa hình bằng các cột có kiểu -- thêm lại ở đây là đi ngược.
 */
public class PaymentRecord {

    private UUID id;
    private UUID orderId;
    private BigDecimal amountVnd;
    private PaymentMethod method;
    private PaymentProvider provider;
    private PaymentStatus status;
    /** Mã giao dịch phía cổng -- duy nhất theo từng lần thử, khóa tra ngược dashboard PayOS/SePay. */
    private String providerOrderRef;
    /** Thời điểm cổng ghi nhận giao dịch, KHÁC createdAt (lúc mình phát link). Null khi chưa trả. */
    private Instant paidAt;
    private Instant createdAt;

    public PaymentRecord() {}

    public PaymentRecord(UUID id, UUID orderId, BigDecimal amountVnd, PaymentMethod method, PaymentProvider provider,
            PaymentStatus status, String providerOrderRef, Instant paidAt, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.amountVnd = amountVnd;
        this.method = method;
        this.provider = provider;
        this.status = status;
        this.providerOrderRef = providerOrderRef;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
    }

    public PaymentRecord(UUID orderId, BigDecimal amountVnd, PaymentMethod method, PaymentProvider provider,
            PaymentStatus status, String providerOrderRef, Instant paidAt, Instant createdAt) {
        this.orderId = orderId;
        this.amountVnd = amountVnd;
        this.method = method;
        this.provider = provider;
        this.status = status;
        this.providerOrderRef = providerOrderRef;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmountVnd() {
        return amountVnd;
    }

    public void setAmountVnd(BigDecimal amountVnd) {
        this.amountVnd = amountVnd;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getProviderOrderRef() {
        return providerOrderRef;
    }

    public void setProviderOrderRef(String providerOrderRef) {
        this.providerOrderRef = providerOrderRef;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** Đã kết thúc (không còn chờ cổng) -- chốt chặn trước khi cho phát link mới. */
    public boolean isSettled() {
        return status != PaymentStatus.PENDING;
    }
}
