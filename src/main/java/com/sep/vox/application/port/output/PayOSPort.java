package com.sep.vox.application.port.output;

import java.math.BigDecimal;
import java.util.Map;

import com.sep.vox.application.response.output.PaymentLinkResult;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;

public interface PayOSPort {
    boolean verifyWebhookSignature(Map<String, Object> data, String signature);

    PaymentLinkResult createPaymentLink(long orderCode, BigDecimal amount, String description);

    PaymentLinkStatusResult getPaymentLinkStatus(long orderCode);
}