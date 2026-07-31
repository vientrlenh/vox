package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public class Invoice {
    private UUID id;
    private String invoiceNumber;
    private UUID schoolId;
    private UUID subscriptionId;
    private InvoiceSourceType sourceType;
    private UUID sourceId;
    private LocalDate issueDate;
    private BigDecimal amount;
    private InvoiceStatus status;
    private Long payosOrderCode;
    private String paymentLinkId;
    private String checkoutUrl;
    private Instant paidAt;

    public Invoice() {}

    public Invoice(UUID id, String invoiceNumber, UUID schoolId, UUID subscriptionId, InvoiceSourceType sourceType, UUID sourceId,
            LocalDate issueDate, BigDecimal amount, InvoiceStatus status, Long payosOrderCode, String paymentLinkId,
            String checkoutUrl, Instant paidAt) {
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
    }

    public Invoice(String invoiceNumber, UUID schoolId, UUID subscriptionId, InvoiceSourceType sourceType, UUID sourceId,
            LocalDate issueDate, BigDecimal amount, InvoiceStatus status, Long payosOrderCode, String paymentLinkId,
            String checkoutUrl, Instant paidAt) {
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

    public InvoiceSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(InvoiceSourceType sourceType) {
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

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
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
}
