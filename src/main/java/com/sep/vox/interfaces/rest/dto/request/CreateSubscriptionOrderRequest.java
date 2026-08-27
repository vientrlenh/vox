package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Không có schoolId: trường lấy từ token của school admin đang đăng nhập -- xem
 * {@link com.sep.vox.application.port.input.command.CreateSubscriptionOrderCommand}.
 */
public record CreateSubscriptionOrderRequest(
    @NotNull(message = "Gói đăng ký muốn mua không được để trống")
    UUID subscriptionPlanId
) {
}
