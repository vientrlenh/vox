package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record BuyTokensCommand(
    UUID schoolId,
    UUID subscriptionId,
    List<TokenPurchaseItemInput> items,
    String idempotencyKey
) {
}
