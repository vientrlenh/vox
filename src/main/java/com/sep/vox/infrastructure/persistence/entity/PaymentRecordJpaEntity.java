package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * MỘT LẦN THỬ thanh toán. Dòng sinh ra lúc phát link (PENDING) và chỉ được đổi đúng hai cột khi cổng
 * báo kết quả: {@code status} và {@code paid_at}. Mọi cột còn lại {@code updatable = false} -- số
 * tiền, cổng, mã đơn đã chốt lúc phát link thì không được sửa, sai thì là một lần thử KHÁC.
 *
 * <p>Trả lại sau khi thất bại sinh dòng MỚI với mã đơn mới: cả PayOS lẫn SePay đều bắt mã đơn phía
 * họ là duy nhất, nên không có chuyện dùng lại mã cũ cho lần thử sau.
 */
@Entity
@Table(name = "payment_records")
public class PaymentRecordJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "amount_vnd", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal amountVnd;

    @Column(name = "method", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_payment_records_method_valid",
            constraint = "method IN ('E_BANKING', 'CARD')"
        )
    })
    private String method;

    @Column(name = "provider", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_payment_records_provider_valid",
            constraint = "provider IN ('PAYOS', 'SEPAY')"
        )
    })
    private String provider;

    // KHÔNG updatable=false: đây là cột duy nhất mang vòng đời của lần thử (PENDING -> PAID/FAILED).
    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_payment_records_status_valid",
            constraint = "status IN ('PENDING', 'PAID', 'FAILED')"
        )
    })
    private String status;

    // Khóa đối soát ngược với dashboard cổng. Unique cùng provider: cùng một giao dịch phía cổng
    // không được ghi nhận hai lần khi webhook và PendingInvoiceReconciler cùng xử lý.
    @Column(name = "provider_order_ref", nullable = false, updatable = false, length = 100)
    private String providerOrderRef;

    // Thời điểm CỔNG ghi nhận giao dịch, khác created_at (lúc mình PHÁT LINK). Null tới khi lần thử
    // này ra tiền, nên phải updatable -- điền cùng lúc với status = PAID.
    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentRecordJpaEntity() {}

    public PaymentRecordJpaEntity(UUID id, UUID orderId, BigDecimal amountVnd, String method, String provider,
            String status, String providerOrderRef, Instant paidAt, Instant createdAt) {
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
