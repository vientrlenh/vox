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
 * Append-only: kết quả cổng đã báo về thì không sửa nữa. Trả lại lần nữa sẽ sinh dòng MỚI.
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

    @Column(name = "status", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_payment_records_status_valid",
            constraint = "status IN ('PAID', 'FAILED')"
        )
    })
    private String status;

    // Khóa đối soát ngược với dashboard cổng. Unique cùng provider: cùng một giao dịch phía cổng
    // không được ghi nhận hai lần khi webhook và PendingInvoiceReconciler cùng xử lý.
    @Column(name = "provider_order_ref", nullable = false, updatable = false, length = 100)
    private String providerOrderRef;

    // Thời điểm CỔNG ghi nhận giao dịch, khác created_at (lúc mình nhận được webhook) -- lệch nhau
    // vài giây tới vài phút nếu webhook bị retry.
    @Column(name = "paid_at", updatable = false)
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
