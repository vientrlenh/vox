package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.payment.PaymentMethod;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;

/**
 * ONE payment ATTEMPT of an order as the school sees it. An order can have several rows (a failed
 * payment followed by a retry), and failed attempts are INTENTIONALLY not hidden: when the school
 * asks "why was I charged only once when I pressed pay twice", this list is the answer.
 *
 * <p>{@code providerPayloadJson} is NOT exposed. It is raw data specific to each gateway, meaningful
 * only to the internal reconciliation path, and because it is a free-form JSON column nothing
 * guarantees that a later gateway version will not put something in it that should not leak -- a field
 * forgotten in a response is even harder to notice than a field that never existed.
 *
 * <p>{@code providerOrderRef}, on the OTHER hand, is exposed on purpose: when the school calls
 * support because "the money was transferred but the order shows nothing", this is exactly the code
 * both sides use to look up one transaction on the PayOS/SePay dashboard.
 */
public record PaymentDto(
    UUID id,
    UUID orderId,
    BigDecimal amountVnd,
    PaymentMethod method,
    PaymentProvider provider,
    PaymentStatus status,
    String providerOrderRef,
    String checkoutUrl,
    Instant paidAt,
    Instant createdAt
) {

    public static PaymentDto toDto(PaymentRecord payment) {
        return new PaymentDto(
            payment.getId(),
            payment.getOrderId(),
            payment.getAmountVnd(),
            payment.getMethod(),
            payment.getProvider(),
            payment.getStatus(),
            payment.getProviderOrderRef(),
            checkoutUrlOf(payment),
            payment.getPaidAt(),
            payment.getCreatedAt()
        );
    }

    /**
     * The link is returned ONLY while the attempt is still pending. The session of a settled attempt
     * is already closed on the gateway side, so exposing that link leads to two outcomes: the
     * frontend builds a "continue paying" button that errors when pressed, or, worse, invites the
     * school to pay again for an order that has already been collected.
     */
    private static String checkoutUrlOf(PaymentRecord payment) {
        return payment.isSettled() ? null : payment.getCheckoutUrl();
    }
}
