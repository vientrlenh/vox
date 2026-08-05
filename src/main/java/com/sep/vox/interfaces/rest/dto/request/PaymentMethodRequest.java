package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record PaymentMethodRequest(
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    String paymentMethod
) {
    public static final List<String> VALID_METHODS = List.of(
        "MANUAL", 
        "PAYOS", 
        "SEPAY"
    );

    @AssertTrue(message = "Phương thức thanh toán không được hỗ trợ")
    public boolean isValidMethod() {
        return VALID_METHODS.contains(paymentMethod);
    }
}
