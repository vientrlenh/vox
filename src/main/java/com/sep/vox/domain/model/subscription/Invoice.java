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
    // Cổng thanh toán đã tạo hóa đơn này. Quyết định adapter nào được dùng để hỏi trạng thái
    // (PendingInvoiceReconciler) và xác thực callback nào áp cho nó.
    private PaymentMethod paymentProvider;
    // Mã đơn theo cách đánh của chính cổng đó (PayOS: orderCode dạng số; SePay PG:
    // order_invoice_number dạng chuỗi). Null với hóa đơn MANUAL.
    private String providerOrderRef;
    private String paymentLinkId;
    private String checkoutUrl;
    private Instant paidAt;
    private UUID resolvedPlanId;

    public Invoice() {}

    public Invoice(UUID id, String invoiceNumber, UUID schoolId, UUID subscriptionId, InvoiceSourceType sourceType, UUID sourceId,
            LocalDate issueDate, BigDecimal amount, InvoiceStatus status, PaymentMethod paymentProvider,
            String providerOrderRef, String paymentLinkId, String checkoutUrl, Instant paidAt, UUID resolvedPlanId) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.issueDate = issueDate;
        this.amount = amount;
        this.status = status;
        this.paymentProvider = paymentProvider;
        this.providerOrderRef = providerOrderRef;
        this.paymentLinkId = paymentLinkId;
        this.checkoutUrl = checkoutUrl;
        this.paidAt = paidAt;
        this.resolvedPlanId = resolvedPlanId;
    }

    public Invoice(String invoiceNumber, UUID schoolId, UUID subscriptionId, InvoiceSourceType sourceType, UUID sourceId,
            LocalDate issueDate, BigDecimal amount, InvoiceStatus status, PaymentMethod paymentProvider,
            String providerOrderRef, String paymentLinkId, String checkoutUrl, Instant paidAt, UUID resolvedPlanId) {
        this.invoiceNumber = invoiceNumber;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.issueDate = issueDate;
        this.amount = amount;
        this.status = status;
        this.paymentProvider = paymentProvider;
        this.providerOrderRef = providerOrderRef;
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

    public PaymentMethod getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(PaymentMethod paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getProviderOrderRef() {
        return providerOrderRef;
    }

    public void setProviderOrderRef(String providerOrderRef) {
        this.providerOrderRef = providerOrderRef;
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
