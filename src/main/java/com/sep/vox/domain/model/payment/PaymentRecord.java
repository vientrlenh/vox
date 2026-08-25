package com.sep.vox.domain.model.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lưu lại các lịch sử tạo hướng thanh toán
 * Đối với PayOS, mặc dù hỗ trợ thanh toán lại, nhưng nếu một yêu cầu thanh toán bị hủy, vẫn phải lưu lại là thất bại
 * Và một đường dẫn thanh toán cho dù là phương thức nào, cũng chỉ hỗ trợ trong 15 phút
 * Qua thời gian đó, mặc định đơn hàng (Order) sẽ là expired
 * PaymentRecord
 */
public class PaymentRecord {
    private UUID id;
    private UUID orderId;
    private BigDecimal amountVnd;
    private PaymentMethod method;
    private PaymentProvider provider;
    private PaymentStatus status;
    private Instant createdAt;

    public PaymentRecord() {}

    public PaymentRecord(UUID id, UUID orderId, BigDecimal amountVnd, PaymentMethod method, PaymentProvider provider,
            PaymentStatus status, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.amountVnd = amountVnd;
        this.method = method;
        this.provider = provider;
        this.status = status;
        this.createdAt = createdAt;
    }

    public PaymentRecord(UUID orderId, BigDecimal amountVnd, PaymentMethod method, PaymentProvider provider,
            PaymentStatus status, Instant createdAt) {
        this.orderId = orderId;
        this.amountVnd = amountVnd;
        this.method = method;
        this.provider = provider;
        this.status = status;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    
}
