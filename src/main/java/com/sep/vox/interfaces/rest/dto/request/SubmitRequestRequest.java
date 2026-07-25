package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.domain.model.subscription.RequestType;

import jakarta.validation.constraints.NotNull;

public record SubmitRequestRequest(
    @NotNull RequestType requestType,
    UUID currentPlanId,
    @NotNull UUID requestedPlanId
) {
}
