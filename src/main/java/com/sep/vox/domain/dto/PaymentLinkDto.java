package com.sep.vox.domain.dto;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.response.output.CheckoutAction;

/**
 * @param providerOrderRef mã đơn phía cổng, dạng chuỗi chứ không phải Long: orderCode dạng số là
 *                         quy ước riêng của PayOS, để kiểu Long thì mọi cổng mới đều phải bẻ mã
 *                         của mình về số
 * @param fields           các field ẩn cần POST khi {@code action} là FORM_POST — có chứa chữ ký
 *                         nên không được ghi vào log
 */
public record PaymentLinkDto(
    UUID invoiceId,
    String providerOrderRef,
    CheckoutAction action,
    String actionUrl,
    String paymentLinkId,
    Map<String, String> fields
) {
}
