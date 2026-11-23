package com.sep.vox.interfaces.rest.dto.request.payment;

import java.util.UUID;

import com.sep.vox.domain.model.payment.PaymentProvider;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentCheckoutUrlRequest(
    @NotNull(message = "Đơn hàng cần thanh toán không được để trống")
    UUID orderId,

    @NotNull(message = "Cổng thanh toán không được để trống")
    PaymentProvider provider
) {
}
