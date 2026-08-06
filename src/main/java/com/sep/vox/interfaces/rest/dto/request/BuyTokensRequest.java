package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BuyTokensRequest(
    @NotNull UUID subscriptionId,
    @NotEmpty List<TokenPurchaseItemRequestItem> items, 

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    String paymentMethod
) {
    private static final List<String> VALID_METHODS = List.of(
        "MANUAL", 
        "PAYOS", 
        "SEPAY"
    );

    @AssertTrue(message = "Phương thức thanh toán không hợp lệ")
    public boolean isPaymentMethodValid() {
        return VALID_METHODS.contains(paymentMethod);
    }
}
