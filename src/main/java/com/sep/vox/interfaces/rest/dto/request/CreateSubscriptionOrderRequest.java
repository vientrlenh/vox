package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateSubscriptionOrderRequest(
    @NotNull(message = "Gói đăng ký muốn mua không được để trống")
    UUID subscriptionPlanId
) {
}
