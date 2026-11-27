package com.sep.vox.application.port.input.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PaymentProcessPort;
import com.sep.vox.domain.model.payment.PaymentProvider;

// Picks the right adapter by PaymentMethod, instead of the use case injecting one concrete implementation directly.
// Indexed by PaymentProcessPort.provider() rather than by a string bean name: a mistyped bean
// name only shows up at runtime, while a wrong enum is caught by the compiler right away. Adding a new gateway = adding 1 class
// that implements PaymentProcessPort, with nothing to change here.
@Service
public class PaymentProcessResolver {

    private final Map<PaymentProvider, PaymentProcessPort> paymentProcessors;

    public PaymentProcessResolver(List<PaymentProcessPort> paymentProcessors) {
        this.paymentProcessors = new EnumMap<>(PaymentProvider.class);
        for (PaymentProcessPort processor : paymentProcessors) {
            PaymentProcessPort existing = this.paymentProcessors.put(processor.provider(), processor);
            if (existing != null) {
                throw new IllegalStateException(
                    "Two adapters declare the same provider() = " + processor.provider() + ": "
                        + existing.getClass().getName() + " and " + processor.getClass().getName());
            }
        }
    }

    public PaymentProcessPort resolve(PaymentProvider provider) {
        PaymentProcessPort port = paymentProcessors.get(provider);
        if (port == null) {
            throw new IllegalArgumentException("Payment gateway not supported: " + provider);
        }
        return port;
    }
}
