package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Chỉ đơn đã hoàn thành mới xuất hóa đơn. Hóa đơn KHÔNG còn mang vòng đời thanh toán (đã chuyển sang
 * Order) lẫn phiên cổng (đã chuyển sang PaymentRecord) -- nó chỉ là chứng từ phát hành sau khi tiền
 * đã về.
 *
 * <p>Append-only: mọi cột {@code updatable = false}. Hóa đơn đã phát hành không được sửa; sai thì
 * phát hành hóa đơn điều chỉnh mới.
 */
@Entity
@Table(name = "invoices")
public class InvoiceJpaEntity {

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

    // Lần thanh toán THÀNH CÔNG của đơn. Chỉ có đúng một dòng PAID cho mỗi order (xem
    // uq_payment_records_one_paid_per_order), nên cột này không mơ hồ.
    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "invoice_number", nullable = false, updatable = false, length = 255)
    private String invoiceNumber;

    @Column(name = "issue_date", nullable = false, updatable = false)
    private Instant issueDate;

    protected InvoiceJpaEntity() {}

    public InvoiceJpaEntity(UUID id, UUID orderId, UUID paymentId, String invoiceNumber, Instant issueDate) {
        this.id = id;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
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

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Instant getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Instant issueDate) {
        this.issueDate = issueDate;
    }
}
