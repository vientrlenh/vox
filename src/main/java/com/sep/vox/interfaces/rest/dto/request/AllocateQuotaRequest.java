package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import com.sep.vox.domain.model.subscription.DistributionMode;

import jakarta.validation.constraints.NotNull;

public record AllocateQuotaRequest(
    @NotNull DistributionMode mode,
    List<UserQuotaAmountRequest> allocations
) {
}
