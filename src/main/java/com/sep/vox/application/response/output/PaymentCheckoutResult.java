package com.sep.vox.application.response.output;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mọi thứ FE cần để đưa người dùng sang trang thanh toán, đã chuẩn hoá cho mọi cổng.
 *
 * @param action        redirect hay submit form — xem {@link CheckoutAction}
 * @param actionUrl     đích của redirect (REDIRECT) hoặc thuộc tính action của form (FORM_POST)
 * @param paymentLinkId định danh link phía cổng, để đối chiếu ngược với dashboard; null với cổng
 *                      không có khái niệm này (SePay PG định danh đơn bằng order_invoice_number)
 * @param fields        các field ẩn của form, rỗng khi action là REDIRECT. Thứ tự các key là một
 *                      phần của hợp đồng chứ không phải chi tiết trình bày: SePay ký trên chuỗi
 *                      ghép theo đúng thứ tự đó, nên form phải POST y nguyên thứ tự này. Có chứa
 *                      chữ ký nên không được ghi vào log
 * @param qrCode        chuỗi VietQR để FE tự vẽ mã, null với cổng không trả về QR
 * @param transfer      thông tin chuyển khoản tay đi kèm mã QR, null khi không có QR
 */
public record PaymentCheckoutResult(
    CheckoutAction action,
    String actionUrl,
    String paymentLinkId,
    Map<String, String> fields,
    String qrCode,
    BankTransferDetails transfer
) {

    public static PaymentCheckoutResult redirect(String checkoutUrl, String paymentLinkId) {
        return new PaymentCheckoutResult(
            CheckoutAction.REDIRECT, checkoutUrl, paymentLinkId, Map.of(), null, null);
    }

    /**
     * Cổng vừa trả về mã QR vừa có trang checkout riêng: hiện mã trong ứng dụng, giữ URL làm lối
     * thoát. Không có QR thì đừng dùng factory này — dùng {@link #redirect} để FE khỏi phải đoán.
     */
    public static PaymentCheckoutResult qr(
            String qrCode, BankTransferDetails transfer, String checkoutUrl, String paymentLinkId) {
        return new PaymentCheckoutResult(
            CheckoutAction.QR, checkoutUrl, paymentLinkId, Map.of(), qrCode, transfer);
    }

    // LinkedHashMap chứ không phải Map.copyOf: Map.copyOf trả về map không có thứ tự xác định, mà
    // ở đây thứ tự chính là cái quyết định chữ ký khớp hay không.
    public static PaymentCheckoutResult formPost(String actionUrl, Map<String, String> fields) {
        return new PaymentCheckoutResult(
            CheckoutAction.FORM_POST, actionUrl, null,
            Collections.unmodifiableMap(new LinkedHashMap<>(fields)), null, null);
    }
}
