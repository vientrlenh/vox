package com.sep.vox.domain.model.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kết quả thanh toán do cổng báo về. CHỈ được tạo khi đã nhận webhook (hoặc khi
 * PendingInvoiceReconciler dò thấy kết quả) -- không có bản ghi nào cho đường dẫn thanh toán chưa ai
 * trả. Vòng đời chờ/hết hạn/hủy của một yêu cầu thanh toán nằm ở Order.
 *
 * <p>Một Order có thể có NHIỀU PaymentRecord (PayOS cho trả lại sau khi thất bại), nhưng chỉ được
 * có TỐI ĐA MỘT dòng PAID -- xem unique index uq_payment_records_one_paid_per_order. Không có ràng
 * buộc đó thì việc trả trùng sẽ ghi hai lần vào số dư.
 */
public class PaymentRecord {

    private UUID id;
    private UUID orderId;
    private BigDecimal amountVnd;
    private PaymentMethod method;
    private PaymentProvider provider;
    private PaymentStatus status;
    /** Mã giao dịch phía cổng -- khóa đối soát ngược với dashboard PayOS/SePay. */
    private String providerOrderRef;
    /** Thời điểm cổng ghi nhận giao dịch, KHÁC createdAt (lúc mình ghi nhận webhook). */
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
}
