package com.sep.vox.domain.model.invoice;

import java.time.Instant;
import java.util.UUID;

/**
 * Chỉ có các đơn hoàn thành mới xuất hóa đơn (chuẩn hóa nghiệp vụ)
 * Loại bỏ việc sử dụng hóa đơn để ghi quá trình thực hiện thanh toán đơn hàng (trước đây fix cứng trong một đơn đăng ký)
 * Mọi thao tác trên một yêu cầu đều chuyển qua thực thể Orders
 * Giờ sẽ gồm luôn cả phần hóa đơn của yêu cầu nạp tiền vào tài khoản nhà trường (được sử dụng để thanh toán cho phần quota của gói bị tiêu thụ hết)
 * Invoice
 */
public class Invoice {
    private UUID id; 
    private UUID orderId;
    private UUID paymentId; // Chỉ lấy lần thanh toán thành công mới nhất
    private String invoiceNumber;
    private Instant issueDate;

    public Invoice() {}

    public Invoice(UUID id, UUID orderId, UUID paymentId, String invoiceNumber, Instant issueDate) {
        this.id = id;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
    }

    public Invoice(UUID orderId, UUID paymentId, String invoiceNumber, Instant issueDate) {
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
