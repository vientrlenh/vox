package com.sep.vox.domain.model.payment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * MỘT LẦN THỬ thanh toán của một Order -- sinh ra ngay lúc phát link (PENDING), rồi chuyển sang
 * PAID/FAILED khi cổng báo về hoặc khi mình chủ động hỏi lại.
 *
 * <p>Một Order có NHIỀU PaymentRecord (trả hụt rồi trả lại), nhưng tối đa MỘT dòng PAID
 * (uq_payment_records_one_paid_per_order) và tối đa MỘT dòng PENDING
 * (uq_payment_records_one_pending_per_order). Ràng buộc PENDING là thứ ép đúng thứ tự: muốn phát
 * link mới thì phải chốt lần thử cũ trước, tức là phải hỏi cổng xem nó đã trả tiền chưa -- không có
 * nó thì trường bấm "thanh toán lại" nhiều lần và trả tiền hai lần cho một đơn.
 *
 * <p>{@code providerOrderRef} là KHÓA ĐỐI SOÁT với cổng và KHÔNG BAO GIỜ dùng lại: cả hai cổng đều
 * bắt mã đơn duy nhất phía họ (PayOS trả lỗi "Đơn thanh toán đã tồn tại"; SePay yêu cầu
 * {@code order_invoice_number} không trùng), nên mỗi lần thử phải mang một mã mới.
 *
 * <p>CỐ Ý không lưu checkoutUrl lẫn paymentLinkId, và cũng KHÔNG có cột payload/metadata dạng JSON
 * cho dữ liệu riêng của từng cổng. Mọi thao tác với cổng đều chỉ cần {@code providerOrderRef}:
 * PayOS tra theo orderCode, SePay tra theo order_invoice_number, và dashboard của cả hai đều tìm
 * được bằng chính mã đó. Một cột JSON tự do ở bảng tiền là đúng thứ V2 vừa bỏ đi khi thay cặp
 * (source_type, source_id) đa hình bằng các cột có kiểu -- thêm lại ở đây là đi ngược.
 */
public class PaymentRecord {

    /**
     * Cửa sổ ân hạn trước khi được phép tin một câu NOT_FOUND từ cổng -- xem
     * {@link #canRetireOnGatewayNotFound}.
     *
     * <p>Đặt dài hơn hẳn mọi timeout gọi cổng (SePay: 5s kết nối + 15s đọc; SDK PayOS không công bố
     * ngưỡng nên phải chừa rộng) vì nó phải trùm được TOÀN BỘ quãng createPaymentLink còn đang bay.
     * Đắt nhất khi đặt rộng chỉ là dòng mồ côi bị dọn ở lượt đối soát sau -- 5 phút nữa; đặt hẹp thì
     * cái giá là một khoản tiền vào mà không giao gì.
     */
    private static final Duration ORPHAN_GRACE = Duration.ofMinutes(5);

    private UUID id;
    private UUID orderId;
    private BigDecimal amountVnd;
    private PaymentMethod method;
    private PaymentProvider provider;
    private PaymentStatus status;
    /** Mã giao dịch phía cổng -- duy nhất theo từng lần thử, khóa tra ngược dashboard PayOS/SePay. */
    private String providerOrderRef;
    /** Thời điểm cổng ghi nhận giao dịch, KHÁC createdAt (lúc mình phát link). Null khi chưa trả. */
    // Link đã phát cho lần thử này -- giữ lại để trường bấm lại thì nhận đúng link cũ.
    private String checkoutUrl;
    // Dữ liệu CHỈ RIÊNG một cổng mới có (vd PayOS paymentLinkId), dạng JSON. Không tách cột riêng
    // cho từng cổng -- xem cột provider_payload_json trong V2. KHÔNG chứa chữ ký.
    private String providerPayloadJson;
    private Instant paidAt;
    private Instant createdAt;

    public PaymentRecord() {}

    public PaymentRecord(UUID id, UUID orderId, BigDecimal amountVnd, PaymentMethod method, PaymentProvider provider,
            PaymentStatus status, String providerOrderRef, String checkoutUrl, String providerPayloadJson,
            Instant paidAt, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.amountVnd = amountVnd;
        this.method = method;
        this.provider = provider;
        this.status = status;
        this.providerOrderRef = providerOrderRef;
        this.checkoutUrl = checkoutUrl;
        this.providerPayloadJson = providerPayloadJson;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
    }

    public PaymentRecord(UUID orderId, BigDecimal amountVnd, PaymentMethod method, PaymentProvider provider,
            PaymentStatus status, String providerOrderRef, String checkoutUrl, String providerPayloadJson,
            Instant paidAt, Instant createdAt) {
        this.orderId = orderId;
        this.amountVnd = amountVnd;
        this.method = method;
        this.provider = provider;
        this.status = status;
        this.providerOrderRef = providerOrderRef;
        this.checkoutUrl = checkoutUrl;
        this.providerPayloadJson = providerPayloadJson;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
    }

    /**
     * Một lần thử thanh toán VỪA MỞ: chưa có tiền, chưa có link, chưa có mốc trả.
     *
     * <p>method đóng cứng E_BANKING vì hiện chỉ nhận thanh toán qua cổng thứ ba (PayOS/SePay) --
     * CARD là thanh toán thẻ trực tiếp, chưa hỗ trợ. Ghi sẵn một method mà chưa chắc đúng rồi chờ
     * callback đính chính là tạo ra một quãng thời gian mà số liệu theo phương thức bị sai; ở đây
     * E_BANKING đúng với mọi đơn đi qua cổng nên không có gì phải đoán.
     *
     * <p>checkoutUrl để null: chỉ có sau khi cổng trả về, chỗ gọi tự set rồi lưu lại.
     */
    public static PaymentRecord forEBankingCheckout(UUID orderId, BigDecimal amountVnd, PaymentProvider provider,
            String providerOrderRef, Instant now) {
        return new PaymentRecord(orderId, amountVnd, PaymentMethod.E_BANKING, provider, PaymentStatus.PENDING,
            providerOrderRef, null, null, null, now);
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

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getProviderOrderRef() {
        return providerOrderRef;
    }

    public void setProviderOrderRef(String providerOrderRef) {
        this.providerOrderRef = providerOrderRef;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getProviderPayloadJson() {
        return providerPayloadJson;
    }

    public void setProviderPayloadJson(String providerPayloadJson) {
        this.providerPayloadJson = providerPayloadJson;
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

    /** Đã kết thúc (không còn chờ cổng) -- chốt chặn trước khi cho phát link mới. */
    public boolean isSettled() {
        return status != PaymentStatus.PENDING;
    }

    /**
     * Có được phép tin lời cổng nói "không có phiên nào mang mã này" mà chốt lần thử này là hỏng hay
     * không.
     *
     * <p>Cần thiết vì dòng PENDING nay được COMMIT TRƯỚC lúc gọi createPaymentLink (xem
     * CreatePaymentCheckoutUrlUseCase): trong suốt quãng lời gọi đó còn đang bay, cổng CHƯA dựng
     * phiên nên trả NOT_FOUND là hoàn toàn đúng -- mà lần thử ấy thì sắp sống. Chốt hỏng nó ngay lúc
     * này tạo ra đúng ca tệ nhất hệ thống có: trường nhận được link thật, trả tiền vào đó, còn dòng
     * ứng với khoản tiền ấy đã mang FAILED nên callback không giao gì cả (xem
     * CallbackOutcome.PAID_AFTER_WRITE_OFF). Cửa sổ này hẹp nhưng có thật -- job đối soát chạy nền
     * hoàn toàn có thể rơi vào giữa hai bước đó.
     *
     * <p>Quá {@code ORPHAN_GRACE} thì không còn lời gọi nào bay được nữa, nên NOT_FOUND lúc đó nghĩa
     * là phiên chưa từng dựng xong: không có đường nào ra tiền theo mã ấy, chốt hỏng là an toàn.
     *
     * <p>Thiếu createdAt thì trả false: chưa biết lần thử này bao nhiêu tuổi thì không được phép
     * đánh hỏng nó.
     */
    public boolean canRetireOnGatewayNotFound(Instant now) {
        return createdAt != null && !now.isBefore(createdAt.plus(ORPHAN_GRACE));
    }
}
