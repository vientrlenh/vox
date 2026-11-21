package com.sep.vox.domain.model.payment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * ONE PAYMENT ATTEMPT of an Order -- created the moment the link is issued (PENDING), then moved to PAID/FAILED when
 * the gateway reports back or when we actively ask again.
 *
 * <p>An Order has MANY PaymentRecords (a failed payment followed by a retry), but at most ONE PAID row
 * (uq_payment_records_one_paid_per_order) and at most ONE PENDING row (uq_payment_records_one_pending_per_order). The
 * PENDING constraint is what enforces the right order of events: to issue a new link the old attempt must be settled
 * first, which means asking the gateway whether it was paid -- without it a school could press "pay again" several
 * times and pay twice for one order.
 *
 * <p>{@code providerOrderRef} is the RECONCILIATION KEY with the gateway and is NEVER reused: both gateways require an
 * order code that is unique on their side (PayOS returns the error "Payment order already exists"; SePay requires a
 * unique {@code order_invoice_number}), so every attempt must carry a new code.
 *
 * <p>checkoutUrl and paymentLinkId are DELIBERATELY not stored, and there is also NO JSON payload/metadata column for
 * gateway-specific data. Every operation against the gateway only needs {@code providerOrderRef}: PayOS looks up by
 * orderCode, SePay looks up by order_invoice_number, and both dashboards can find it with that same code. A free-form
 * JSON column on a money table is exactly what V2 just removed when it replaced the polymorphic (source_type,
 * source_id) pair with typed columns -- adding it back here would go against that.
 */
public class PaymentRecord {

    /**
     * Grace window before a NOT_FOUND answer from the gateway may be trusted -- see
     * {@link #canRetireOnGatewayNotFound}.
     *
     * <p>Set much longer than every gateway call timeout (SePay: 5s connect + 15s read; the PayOS SDK does not publish a
     * threshold, so leave plenty of room) because it must cover the WHOLE time a createPaymentLink call may still be in
     * flight. The worst cost of setting it wide is an orphan row cleaned up in the next reconciliation round -- 5 minutes
     * later; setting it narrow costs money coming in without anything being delivered.
     */
    private static final Duration ORPHAN_GRACE = Duration.ofMinutes(5);

    private UUID id;
    private UUID orderId;
    private BigDecimal amountVnd;
    private PaymentMethod method;
    private PaymentProvider provider;
    private PaymentStatus status;
    /** Transaction code on the gateway side -- unique per attempt, the key for looking up the PayOS/SePay dashboards. */
    private String providerOrderRef;
    /** The moment the gateway recorded the transaction, DIFFERENT from createdAt (when we issued the link). Null until paid. */
    // The link issued for this attempt -- kept so that when the school clicks again it receives the same old link.
    private String checkoutUrl;
    // Data that ONLY ONE gateway has (e.g. the PayOS paymentLinkId), as JSON. No separate column per gateway -- see the
    // provider_payload_json column in V2. Does NOT contain signatures.
    private String providerPayloadJson;
    private Instant paidAt;
    private Instant createdAt;

    public PaymentRecord() {}



    /**
     * A payment attempt JUST OPENED: no money yet, no link yet, no payment timestamp yet.
     *
     * <p>method is hard-coded to E_BANKING because only payment through a third-party gateway (PayOS/SePay) is accepted
     * right now -- CARD is direct card payment, which is not supported yet. Writing a method that may be wrong and waiting
     * for a callback to correct it creates a window in which the per-method figures are wrong; here E_BANKING is correct
     * for every order that goes through the gateway, so there is nothing to guess.
     *
     * <p>checkoutUrl is left null: it only exists after the gateway responds, and the caller sets it and saves again.
     */
    public static PaymentRecord forEBankingCheckout(UUID orderId, BigDecimal amountVnd, PaymentProvider provider,
            String providerOrderRef, Instant now) {
        return builder()
            .orderId(orderId)
            .amountVnd(amountVnd)
            .method(PaymentMethod.E_BANKING)
            .provider(provider)
            .status(PaymentStatus.PENDING)
            .providerOrderRef(providerOrderRef)
            .createdAt(now)
            .build();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmountVnd() {
        return amountVnd;
    }

    public void setAmountVnd(BigDecimal amountVnd) {
        this.amountVnd = amountVnd;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getProviderOrderRef() {
        return providerOrderRef;
    }

    public void setProviderOrderRef(String providerOrderRef) {
        this.providerOrderRef = providerOrderRef;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getProviderPayloadJson() {
        return providerPayloadJson;
    }

    public void setProviderPayloadJson(String providerPayloadJson) {
        this.providerPayloadJson = providerPayloadJson;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** Finished (no longer waiting for the gateway) -- the guard before a new link may be issued. */
    public boolean isSettled() {
        return status != PaymentStatus.PENDING;
    }

    /**
     * Whether the gateway's statement "no session carries this code" may be trusted to settle this attempt as failed.
     *
     * <p>Needed because the PENDING row is now COMMITTED BEFORE createPaymentLink is called (see
     * CreatePaymentCheckoutUrlUseCase): during the time that call is still in flight the gateway has NOT built the
     * session yet, so NOT_FOUND is entirely correct -- yet that attempt is about to be alive. Settling it as failed right
     * now produces the worst case the system has: the school receives a real link, pays into it, and the row matching that
     * money already carries FAILED, so the callback delivers nothing (see CallbackOutcome.PAID_AFTER_WRITE_OFF). The window
     * is narrow but real -- the background reconciliation job can easily fall between those two steps.
     *
     * <p>Past {@code ORPHAN_GRACE} no call can still be in flight, so NOT_FOUND then means the session was never built
     * successfully: there is no way for money to arrive under that code, and settling it as failed is safe.
     *
     * <p>Returns false when createdAt is missing: if we do not know how old the attempt is, we must not mark it failed.
     */
    public boolean canRetireOnGatewayNotFound(Instant now) {
        return createdAt != null && !now.isBefore(createdAt.plus(ORPHAN_GRACE));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final PaymentRecord target = new PaymentRecord();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder orderId(UUID orderId) {
            target.orderId = orderId;
            return this;
        }

        public Builder amountVnd(BigDecimal amountVnd) {
            target.amountVnd = amountVnd;
            return this;
        }

        public Builder method(PaymentMethod method) {
            target.method = method;
            return this;
        }

        public Builder provider(PaymentProvider provider) {
            target.provider = provider;
            return this;
        }

        public Builder status(PaymentStatus status) {
            target.status = status;
            return this;
        }

        public Builder providerOrderRef(String providerOrderRef) {
            target.providerOrderRef = providerOrderRef;
            return this;
        }

        public Builder checkoutUrl(String checkoutUrl) {
            target.checkoutUrl = checkoutUrl;
            return this;
        }

        public Builder providerPayloadJson(String providerPayloadJson) {
            target.providerPayloadJson = providerPayloadJson;
            return this;
        }

        public Builder paidAt(Instant paidAt) {
            target.paidAt = paidAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public PaymentRecord build() {
            return target;
        }

    }
}
