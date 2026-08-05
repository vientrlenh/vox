package com.sep.vox.domain.model.subscription;

public enum PaymentMethod {
    PAYOS,
    SEPAY,
    // Hóa đơn đối soát tay, không đi qua cổng thanh toán nào — providerOrderRef để null.
    MANUAL; 

    public static PaymentMethod resolve(String method) {
        try {
            return PaymentMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Phương thức thanh toán " + method + " không được hỗ trợ");
        }
    }
}