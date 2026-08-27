package com.sep.vox.application.port.input.command;

import java.util.Map;

import com.sep.vox.domain.model.payment.PaymentProvider;

public record ProcessPaymentCallbackCommand(
    PaymentProvider provider, 
    byte[] rawBody, 
    Map<String, String> headers
) {
    
}
