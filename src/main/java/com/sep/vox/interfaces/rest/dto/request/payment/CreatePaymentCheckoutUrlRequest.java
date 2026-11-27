package com.sep.vox.interfaces.rest.dto.request.payment;

import java.util.UUID;

import com.sep.vox.domain.model.payment.PaymentProvider;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentCheckoutUrlRequest(
    @NotNull(message = "Order to pay must not be blank")
    UUID orderId,

    @NotNull(message = "Payment gateway must not be blank")
    PaymentProvider provider
) {
}
