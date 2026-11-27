package com.sep.vox.interfaces.rest.dto.request.payment;

import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

// Only used by the endpoints that create a payment link, so MANUAL is deliberately not in the list:
// a payment method collected outside the system has no gateway to create a link with. Rejecting here only gives an early
// and clear error -- the real guard is PaymentMethod.resolveOnlineGateway() in the use case.
public record PaymentMethodRequest(
    @NotBlank(message = "Payment method must not be blank")
    String paymentMethod
) {
    public static final List<String> VALID_METHODS = List.of(
        "PAYOS",
        "SEPAY"
    );

    @AssertTrue(message = "Payment method is not supported")
    public boolean isValidMethod() {
        return VALID_METHODS.contains(paymentMethod);
    }
}
