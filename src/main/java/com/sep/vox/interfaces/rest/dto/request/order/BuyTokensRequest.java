package com.sep.vox.interfaces.rest.dto.request.order;

import com.sep.vox.application.port.input.command.order.BuyTokensCommand;
import com.sep.vox.application.port.input.command.order.TokenPurchaseItemInput;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BuyTokensRequest(
    @NotNull UUID subscriptionId,
    @NotEmpty List<TokenPurchaseItemRequestItem> items, 

    @NotBlank(message = "Payment method must not be blank")
    String paymentMethod
) {
    private static final List<String> VALID_METHODS = List.of(
        "MANUAL", 
        "PAYOS", 
        "SEPAY"
    );

    @AssertTrue(message = "Payment method is invalid")
    public boolean isPaymentMethodValid() {
        return VALID_METHODS.contains(paymentMethod);
    }

    public static BuyTokensCommand toCommand(UUID schoolId, BuyTokensRequest request) {
        return new BuyTokensCommand(
            schoolId,
            request.subscriptionId(),
            request.items().stream()
                .map(item -> new TokenPurchaseItemInput(item.quotaType(), item.quantity()))
                .toList(), 
            request.paymentMethod()
        );
    }
}
