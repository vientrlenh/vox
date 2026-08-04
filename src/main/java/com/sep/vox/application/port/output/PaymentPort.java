package com.sep.vox.application.port.output;

import java.math.BigDecimal;
import java.util.Map;

import com.sep.vox.application.response.output.PaymentLinkResult;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.subscription.PaymentMethod;

public interface PaymentPort {
    // Cho PaymentPortResolver biết implementation này phục vụ PaymentMethod nào, để chọn đúng bean
    // khi có nhiều cổng thanh toán cùng đăng ký (VD thêm VNPAY sau này).
    PaymentMethod supports();

    boolean verifyWebhookSignature(Map<String, Object> data, String signature);

    PaymentLinkResult createPaymentLink(long orderCode, BigDecimal amount, String description);

    PaymentLinkStatusResult getPaymentLinkStatus(long orderCode);
}