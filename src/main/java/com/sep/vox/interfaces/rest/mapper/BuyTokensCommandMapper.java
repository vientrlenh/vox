package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.BuyTokensCommand;
import com.sep.vox.application.port.input.command.TokenPurchaseItemInput;
import com.sep.vox.interfaces.rest.dto.request.BuyTokensRequest;

public final class BuyTokensCommandMapper {

    private BuyTokensCommandMapper() {
    }

    public static BuyTokensCommand fromRequest(UUID schoolId, BuyTokensRequest request, String idempotencyKey) {
        return new BuyTokensCommand(
            schoolId,
            request.subscriptionId(),
            request.items().stream()
                .map(item -> new TokenPurchaseItemInput(item.quotaType(), item.quantity()))
                .toList(),
            idempotencyKey
        );
    }
}
