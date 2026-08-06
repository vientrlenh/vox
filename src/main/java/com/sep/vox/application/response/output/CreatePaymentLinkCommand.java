package com.sep.vox.application.response.output;

import java.math.BigDecimal;

public record CreatePaymentLinkCommand(
    String orderRef,
    BigDecimal amount,
    String description
) {
    
}
