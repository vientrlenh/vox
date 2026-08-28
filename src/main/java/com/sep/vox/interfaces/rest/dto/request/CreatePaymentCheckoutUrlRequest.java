package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreatePaymentCheckoutUrlRequest(
    @NotNull(message = "Đơn hàng cần thanh toán không được để trống")
    UUID orderId,

    @NotBlank(message = "Cổng thanh toán không được để trống")
    @Pattern(regexp = "^(PAYOS|SEPAY)$", message = "Chỉ chấp nhận cổng thanh toán PAYOS/SEPAY")
    String provider
) {
}
