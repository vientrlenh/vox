package com.sep.vox.application.port.input.command;

import java.util.UUID;

/**
 * Thay cho BA command cũ (CreatePaymentLinkForSubscriptionRequestCommand,
 * CreatePaymentLinkForRenewalCommand, và BuyTokensCommand dùng làm lệnh phát link). Giờ chỉ còn một:
 * mọi thứ phải trả tiền đều đã là một Order, nên phát checkout chỉ cần biết ĐƠN NÀO và QUA CỔNG NÀO.
 *
 * <p>provider nhận chuỗi thô để giữ nguyên hình dạng payload; use case tự parse và tự báo lỗi đọc
 * được thay vì để Spring ném MethodArgumentTypeMismatchException.
 */
public record CreatePaymentCheckoutUrlCommand(
    UUID orderId,
    String provider
) {
}
