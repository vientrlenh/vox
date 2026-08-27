package com.sep.vox.application.response.input.payment;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.domain.model.payment.PaymentProvider;

/**
 * Mọi thứ FE cần để đưa trường sang trang thanh toán.
 *
 * <p>FE nhìn {@code action} để biết phải làm gì, KHÔNG nhìn {@code provider}: provider chỉ để hiển
 * thị ("Thanh toán qua PayOS") và tra soát. Rẽ nhánh theo tên cổng nghĩa là thêm cổng mới lại phải
 * sửa FE -- đúng cái mà {@link com.sep.vox.application.response.output.CheckoutAction} sinh ra để
 * tránh.
 *
 * @param orderId          đơn đang được trả -- KHÔNG phải invoiceId như PaymentLinkDto cũ: hóa đơn
 *                         giờ chỉ phát sau khi tiền về, lúc phát link chưa có hóa đơn nào tồn tại
 * @param paymentId        lần thử thanh toán tương ứng, để FE hỏi lại trạng thái
 * @param providerOrderRef mã đơn phía cổng, dạng chuỗi chứ không phải Long: orderCode dạng số là quy
 *                         ước riêng của PayOS
 * @param fields           field ẩn phải POST khi {@code action} là FORM_POST, rỗng khi REDIRECT.
 *                         Thứ tự key là một phần của hợp đồng (SePay ký trên chuỗi ghép theo đúng
 *                         thứ tự đó). CÓ CHỨA CHỮ KÝ nên không được ghi log và không lưu xuống DB
 */
public record PaymentCheckoutResponse(
    UUID orderId,
    UUID paymentId,
    String providerOrderRef,
    String provider,
    String action,
    String checkoutUrl,
    Map<String, String> fields
) {

    public static PaymentCheckoutResponse from(
            UUID orderId, UUID paymentId, String providerOrderRef, PaymentProvider provider,
            PaymentCheckoutResult result) {
        return new PaymentCheckoutResponse(
            orderId,
            paymentId,
            providerOrderRef,
            provider.name(),
            result.action().name(),
            result.actionUrl(),
            result.fields()
        );
    }

    /**
     * Dựng lại từ một lần thử ĐANG TREO mà ta chỉ còn giữ checkout_url. Luôn là REDIRECT: cổng dạng
     * FORM_POST không tái tạo được ở đây vì bộ field mang chữ ký, mà chữ ký thì cố ý không lưu.
     */
    public static PaymentCheckoutResponse redirectTo(
            UUID orderId, UUID paymentId, String providerOrderRef, PaymentProvider provider, String checkoutUrl) {
        return new PaymentCheckoutResponse(
            orderId, paymentId, providerOrderRef, provider.name(), "REDIRECT", checkoutUrl, Map.of());
    }
}
