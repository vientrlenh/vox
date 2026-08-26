package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.payment.PaymentMethod;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;

/**
 * MỘT LẦN THỬ thanh toán như trường nhìn thấy nó. Một đơn có thể có nhiều dòng (trả hụt rồi trả
 * lại), và các lần thất bại CỐ Ý không bị giấu đi: khi trường thắc mắc "sao tôi bấm trả hai lần mà
 * chỉ trừ một lần", danh sách này chính là câu trả lời.
 *
 * <p>KHÔNG mang {@code providerPayloadJson} ra ngoài. Đó là dữ liệu thô riêng của từng cổng, chỉ có
 * nghĩa với đường đối soát nội bộ, và vì nó là cột JSON tự do nên không có gì đảm bảo cổng sẽ không
 * nhét thêm thứ không nên lộ vào đó ở phiên bản sau -- một trường bị bỏ quên trong response còn khó
 * phát hiện hơn một trường chưa bao giờ có.
 *
 * <p>{@code providerOrderRef} thì NGƯỢC LẠI, cố ý đưa ra: khi trường gọi hỗ trợ vì "đã chuyển tiền
 * mà đơn chưa thấy gì", đây đúng là mã để hai bên cùng tra một giao dịch trên dashboard PayOS/SePay.
 */
public record PaymentDto(
    UUID id,
    UUID orderId,
    BigDecimal amountVnd,
    String method,
    String provider,
    String status,
    String providerOrderRef,
    String checkoutUrl,
    String paidAt,
    String createdAt
) {

    public static PaymentDto toDto(PaymentRecord payment) {
        return new PaymentDto(
            payment.getId(),
            payment.getOrderId(),
            payment.getAmountVnd(),
            valueOf(payment.getMethod()),
            valueOf(payment.getProvider()),
            valueOf(payment.getStatus()),
            payment.getProviderOrderRef(),
            checkoutUrlOf(payment),
            valueOf(payment.getPaidAt()),
            valueOf(payment.getCreatedAt())
        );
    }

    /**
     * Link CHỈ trả ra khi lần thử còn treo. Phiên của một lần thử đã chốt thì bên cổng đã đóng, nên
     * đưa link đó ra chỉ dẫn tới hai kết cục: FE dựng một nút "thanh toán tiếp" bấm vào là lỗi, hoặc
     * tệ hơn, mời trường trả lại tiền cho một đơn đã thu xong.
     */
    private static String checkoutUrlOf(PaymentRecord payment) {
        return payment.isSettled() ? null : payment.getCheckoutUrl();
    }

    private static String valueOf(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static String valueOf(PaymentMethod method) {
        return method == null ? null : method.name();
    }

    private static String valueOf(PaymentProvider provider) {
        return provider == null ? null : provider.name();
    }

    private static String valueOf(PaymentStatus status) {
        return status == null ? null : status.name();
    }
}
