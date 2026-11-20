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
     * mọi callback thành xếp hàng sau nhau.
     *
     * <p>ĐỦ DÀI để không bao giờ phải dựa vào ràng buộc duy nhất. Ràng buộc đó vẫn còn và vẫn chặn,
     * nhưng nó nổ BÊN TRONG transaction của {@code OrderSettlementService.settlePaid} -- kéo theo cả
     * lần thử đã PAID, trạng thái SUCCESS của đơn và phần giao hàng (cộng ví hoặc kích hoạt gói) cùng
     * rollback, cho một đơn mà tiền đã về thật. Không có vòng thử lại nào ở đây, nên trùng số nghĩa
     * là chờ PendingOrderReconciler quay lại gieo xúc xắc hộ.
     *
     * <p>Vì thế lấy 48 bit (12 ký tự hex) thay vì 8 ký tự như bản đầu. Theo nghịch lý ngày sinh, 8 ký
     * tự (~4,3 tỷ giá trị) đã có ~1% khả năng trùng ở mức 10.000 hóa đơn một năm và 50% ở khoảng
     * 77.000 -- hoàn toàn nằm trong tầm với. 48 bit (~2,8 x 10^14) đẩy mốc 50% đó lên khoảng 20 triệu
     * hóa đơn mỗi năm, tức là ca rollback trên thực tế không còn xảy ra.
     *
     * <p>Lấy từ {@code getLeastSignificantBits}: 2 bit variant của UUID v4 nằm ở đỉnh nửa thấp, còn 4
     * bit version nằm trong nửa cao -- mặt nạ 48 bit dưới đây vì thế giữ được 48 bit NGẪU NHIÊN THẬT,
     * trong khi cắt từ nửa cao sẽ dính 4 bit cố định.
     */
    public static Invoice issueFor(UUID orderId, UUID paymentId, Instant issuedAt) {
        var year = issuedAt.atZone(ZoneConstant.BUSINESS_ZONE).getYear();
        var suffix = "%012X".formatted(UUID.randomUUID().getLeastSignificantBits() & 0xFFFF_FFFF_FFFFL);
        return new Invoice(orderId, paymentId, "INV-" + year + "-" + suffix, issuedAt);
    }
}
