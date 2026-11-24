package com.sep.vox.application.port.input.command.payment;

import java.util.UUID;

import com.sep.vox.domain.model.payment.PaymentProvider;

/**
 * Thay cho BA command cũ (CreatePaymentLinkForSubscriptionRequestCommand,
 * CreatePaymentLinkForRenewalCommand, và BuyTokensCommand dùng làm lệnh phát link). Giờ chỉ còn một:
 * mọi thứ phải trả tiền đều đã là một Order, nên phát checkout chỉ cần biết ĐƠN NÀO và QUA CỔNG NÀO.
 */
public record CreatePaymentCheckoutUrlCommand(
    UUID orderId,
    PaymentProvider provider
) {
}
