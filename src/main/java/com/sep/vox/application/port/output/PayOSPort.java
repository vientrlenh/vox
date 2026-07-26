package com.sep.vox.application.port.output;

import java.math.BigDecimal;
import java.util.Map;

import com.sep.vox.application.response.output.PaymentLinkResult;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;

public interface PayOSPort {
    boolean verifyWebhookSignature(Map<String, Object> data, String signature);

    PaymentLinkResult createPaymentLink(long orderCode, BigDecimal amount, String description);

    // Dùng khi cần override returnUrl/cancelUrl mặc định (vd: System Admin duyệt request qua PayOS thì
    // phải quay về trang payment-result của System Admin thay vì School Admin).
    PaymentLinkResult createPaymentLink(long orderCode, BigDecimal amount, String description, String returnUrl, String cancelUrl);

    PaymentLinkStatusResult getPaymentLinkStatus(long orderCode);
}