package com.sep.vox.application.port.input.command.payment;

import java.util.UUID;

import com.sep.vox.domain.model.payment.PaymentProvider;

/**
 * Replaces THREE old commands (CreatePaymentLinkForSubscriptionRequestCommand,
 * CreatePaymentLinkForRenewalCommand, and BuyTokensCommand used as a link-issuing command). Now there is just one:
 * everything that has to be paid for is already an Order, so issuing a checkout only needs to know WHICH ORDER and THROUGH WHICH GATEWAY.
 */
public record CreatePaymentCheckoutUrlCommand(
    UUID orderId,
    PaymentProvider provider
) {
}
