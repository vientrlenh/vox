package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

// Chỉ dùng cho các endpoint tạo link thanh toán, nên MANUAL cố tình không có trong danh sách:
// phương thức thu ngoài hệ thống không có cổng nào để tạo link. Chặn ở đây chỉ để trả lỗi sớm
// và rõ ràng — chốt chặn thật nằm ở PaymentMethod.resolveOnlineGateway() trong use case.
public record PaymentMethodRequest(
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    String paymentMethod
) {
    public static final List<String> VALID_METHODS = List.of(
        "PAYOS",
        "SEPAY"
    );

    @AssertTrue(message = "Phương thức thanh toán không được hỗ trợ")
    public boolean isValidMethod() {
        return VALID_METHODS.contains(paymentMethod);
    }
}
