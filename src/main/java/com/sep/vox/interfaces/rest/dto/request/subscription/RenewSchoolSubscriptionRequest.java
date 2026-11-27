package com.sep.vox.interfaces.rest.dto.request.subscription;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RenewSchoolSubscriptionRequest(
    @NotNull(message = "Confirmed renewal plan must not be blank")
    UUID acceptedPlanId
) {
}
