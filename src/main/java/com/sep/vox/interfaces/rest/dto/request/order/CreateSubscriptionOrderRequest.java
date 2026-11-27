package com.sep.vox.interfaces.rest.dto.request.order;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * There is no schoolId: the school comes from the token of the logged-in school admin -- see
 * {@link com.sep.vox.application.port.input.command.order.CreateSubscriptionOrderCommand}.
 */
public record CreateSubscriptionOrderRequest(
    @NotNull(message = "Subscription plan to buy must not be blank")
    UUID subscriptionPlanId
) {
}
