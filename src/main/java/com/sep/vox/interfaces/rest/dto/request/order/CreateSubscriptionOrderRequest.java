package com.sep.vox.interfaces.rest.dto.request.order;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * There is no schoolId: the school comes from the token of the logged-in school admin -- see
 * {@link com.sep.vox.application.port.input.command.order.CreateSubscriptionOrderCommand}.
 */
public record CreateSubscriptionOrderRequest(
    @NotNull(message = "Gói đăng ký muốn mua không được để trống")
    UUID subscriptionPlanId
) {
}
