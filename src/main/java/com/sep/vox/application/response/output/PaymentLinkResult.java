package com.sep.vox.application.response.output;

public record PaymentLinkResult(
    String paymentLinkId,
    String checkoutUrl
) {
}