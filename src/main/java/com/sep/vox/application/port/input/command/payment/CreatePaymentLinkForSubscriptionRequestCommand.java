package com.sep.vox.application.port.input.command.payment;

import java.util.UUID;

public record CreatePaymentLinkForSubscriptionRequestCommand(
    UUID requestId, 
    String paymentMethod
) {
}