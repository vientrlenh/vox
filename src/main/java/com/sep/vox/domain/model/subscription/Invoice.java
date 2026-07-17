package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class Invoice {
    private UUID id;
    private String invoiceNumber;
    private UUID subscriptionId;
    private InvoiceSourceType sourceType;
    private UUID sourceId;
    private LocalDate issueDate;
    private BigDecimal amount;
    private InvoiceStatus status;

    public Invoice() {}

    public Invoice(UUID id, String invoiceNumber, UUID subscriptionId, InvoiceSourceType sourceType, UUID sourceId,
            LocalDate issueDate, BigDecimal amount, InvoiceStatus status) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.subscriptionId = subscriptionId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.issueDate = issueDate;
        this.amount = amount;
        this.status = status;
    }

    public Invoice(String invoiceNumber, UUID subscriptionId, InvoiceSourceType sourceType, UUID sourceId,
            LocalDate issueDate, BigDecimal amount, InvoiceStatus status) {
        this.invoiceNumber = invoiceNumber;
        this.subscriptionId = subscriptionId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.issueDate = issueDate;
        this.amount = amount;
        this.status = status;
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
}
