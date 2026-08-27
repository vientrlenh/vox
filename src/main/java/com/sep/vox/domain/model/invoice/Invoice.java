package com.sep.vox.domain.model.invoice;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.common.ZoneConstant;

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

    /**
     * Phát hành chứng từ cho một đơn đã thu đủ tiền.
     *
     * <p>Số hóa đơn sinh THEO NĂM CỦA NGÀY PHÁT HÀNH, lấy theo múi giờ nghiệp vụ: phát hành lúc
     * 23:30 ngày 31/12 giờ Việt Nam vẫn còn là năm cũ, trong khi cùng khoảnh khắc đó ở UTC đã sang
     * năm mới -- lấy nhầm sẽ ra hóa đơn số INV-2027-... nhưng ngày ghi 31/12/2026.
     *
     * <p>Phần đuôi lấy từ UUID ngẫu nhiên chứ không phải bộ đếm tăng dần: bộ đếm cần một dòng khóa
     * chung cho cả hệ thống, mà đây là đường chạy trong transaction của webhook -- khóa ở đó biến
     * mọi callback thành xếp hàng sau nhau. Trùng số vẫn bị chặn bởi ràng buộc duy nhất trên cột.
     */
    public static Invoice issueFor(UUID orderId, UUID paymentId, Instant issuedAt) {
        var year = issuedAt.atZone(ZoneConstant.BUSINESS_ZONE).getYear();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new Invoice(orderId, paymentId, "INV-" + year + "-" + suffix, issuedAt);
    }
}
