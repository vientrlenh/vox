package com.sep.vox.interfaces.rest.dto.request.subscription;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RenewSchoolSubscriptionRequest(
    @NotNull(message = "Gói gia hạn đã xác nhận không được để trống")
    UUID acceptedPlanId
) {
}
