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
 */
public record PaymentCheckoutResult(
    CheckoutAction action,
    String actionUrl,
    String paymentLinkId,
    Map<String, String> fields
) {

    public static PaymentCheckoutResult redirect(String checkoutUrl, String paymentLinkId) {
        return new PaymentCheckoutResult(CheckoutAction.REDIRECT, checkoutUrl, paymentLinkId, Map.of());
    }

    // LinkedHashMap chứ không phải Map.copyOf: Map.copyOf trả về map không có thứ tự xác định, mà
    // ở đây thứ tự chính là cái quyết định chữ ký khớp hay không.
    public static PaymentCheckoutResult formPost(String actionUrl, Map<String, String> fields) {
        return new PaymentCheckoutResult(
            CheckoutAction.FORM_POST, actionUrl, null, Collections.unmodifiableMap(new LinkedHashMap<>(fields)));
    }
}
