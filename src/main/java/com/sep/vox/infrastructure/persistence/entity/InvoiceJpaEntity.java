package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice", indexes = {
    @Index(columnList = "school_id", name = "idx_invoice_school")
})
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

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "source_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_invoice_source_type_valid",
            constraint = "source_type IN ('SUBSCRIPTION', 'SUBSCRIPTION_REQUEST', 'TOKEN_PURCHASE')"
        )
    })
    private String sourceType;

    @Column(name = "source_id", nullable = false, updatable = false)
    private UUID sourceId;

    @Column(name = "issue_date", nullable = false, updatable = false)
    private LocalDate issueDate;

    @Column(name = "amount", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_invoice_status_valid",
            constraint = "status IN ('PAID', 'PENDING', 'FAILED', 'CANCELLED')"
        )
    })
    private String status;

    @Column(name = "payos_order_code", unique = true)
    private Long payosOrderCode;

    @Column(name = "payment_link_id")
    private String paymentLinkId;

    @Column(name = "checkout_url", length = 2048)
    private String checkoutUrl;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "resolved_plan_id")
    private UUID resolvedPlanId;

    protected InvoiceJpaEntity() {}

    public InvoiceJpaEntity(UUID id, String invoiceNumber, UUID schoolId, UUID subscriptionId, String sourceType, UUID sourceId,
            LocalDate issueDate, BigDecimal amount, String status, Long payosOrderCode, String paymentLinkId,
            String checkoutUrl, Instant paidAt, UUID resolvedPlanId) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.issueDate = issueDate;
        this.amount = amount;
        this.status = status;
        this.payosOrderCode = payosOrderCode;
        this.paymentLinkId = paymentLinkId;
        this.checkoutUrl = checkoutUrl;
        this.paidAt = paidAt;
        this.resolvedPlanId = resolvedPlanId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPayosOrderCode() {
        return payosOrderCode;
    }

    public void setPayosOrderCode(Long payosOrderCode) {
        this.payosOrderCode = payosOrderCode;
    }

    public String getPaymentLinkId() {
        return paymentLinkId;
    }

    public void setPaymentLinkId(String paymentLinkId) {
        this.paymentLinkId = paymentLinkId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public UUID getResolvedPlanId() {
        return resolvedPlanId;
    }

    public void setResolvedPlanId(UUID resolvedPlanId) {
        this.resolvedPlanId = resolvedPlanId;
    }
}
