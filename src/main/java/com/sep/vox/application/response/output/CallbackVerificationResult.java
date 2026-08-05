package com.sep.vox.application.response.output;

import java.math.BigDecimal;

public record CallbackVerificationResult(
    boolean valid,
    String providerOrderRef,
    PaymentLinkRemoteStatus status,
    BigDecimal amount
) {
    public static CallbackVerificationResult invalid() {
        return new CallbackVerificationResult(false, null, null, null);
    }
}
