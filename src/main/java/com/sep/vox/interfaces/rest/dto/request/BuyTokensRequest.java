package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BuyTokensRequest(
    @NotNull UUID subscriptionId,
    @NotEmpty List<TokenPurchaseItemRequestItem> items
) {
}
