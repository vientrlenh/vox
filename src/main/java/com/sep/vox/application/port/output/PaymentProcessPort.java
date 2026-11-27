package com.sep.vox.application.port.output;

import java.util.Map;

import com.sep.vox.application.response.output.CallbackVerificationResult;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.payment.PaymentProvider;

// The common contract for every payment gateway. Principle: no data type or convention of a
// specific gateway may leak through this interface -- the order code is a string (not PayOS's
// numeric orderCode), and unpacking the callback payload lives entirely inside the adapter, not
// in the application layer.
public interface PaymentProcessPort {

    // The gateway this adapter serves -- PaymentProcessResolver uses it to build its index, instead of
    // looking up a string bean name. It is the PaymentProvider (PAYOS/SEPAY), not the PaymentMethod:
    // method now means WHAT the payer paid WITH (E_BANKING/CARD), not WHERE they paid.
    PaymentProvider provider();

    /**
     * Whether this gateway has enough configuration to be used for real.
     *
     * <p>It must be askable BEFORE a payment_records row is written, which is the whole reason it
     * is on the port rather than sitting quietly in the adapter: the attempt row is committed in a
     * SEPARATE transaction before the gateway is called, so if that call dies because of missing
     * configuration, the PENDING row stays. From then on the order cannot be cancelled
     * (CancelOrderUseCase has to ask the gateway), no new link can be issued, and it cannot expire
     * (expireIfOverdue refuses to close an order while an attempt is pending) -- the order is stuck
     * in PENDING forever, and for a subscription order uq_orders_one_open_subscription_order also
     * locks the school out of buying again.
     *
     * <p>Different from a gateway that is BROKEN: that case already has a recovery path (the gateway
     * returns NOT_FOUND, then PaymentRecord.canRetireOnGatewayNotFound closes the attempt). That path
     * needs the gateway to ANSWER, so it cannot rescue the missing-configuration case -- the only case
     * we know for certain before calling.
     *
     * <p>Deliberately not validated at startup: dev/test environments must run without the
     * environment variables of any gateway.
     */
    boolean isConfigured();

    /**
     * Generates an order code following the gateway's own convention. PayOS requires a numeric
     * orderCode, SePay uses a string in order_invoice_number -- neither gateway can force its
     * convention on the other, so the decision belongs to the adapter.
     *
     * <p>No longer takes invoiceNumber: the invoice is now issued only AFTER the money arrives, so
     * when the link is issued no invoice number exists yet. The way back from a code to the order
     * is payment_records.provider_order_ref.
     *
     * <p>EVERY CALL MUST PRODUCE A NEW CODE, even for the same order: PayOS rejects a duplicate
     * orderCode and SePay requires a unique order_invoice_number, so a retry always gets a new code
     * instead of reusing one.
     */
    String newOrderRef();

    PaymentCheckoutResult createPaymentLink(CreatePaymentLinkCommand command);

    PaymentLinkStatusResult getPaymentLinkStatus(String providerOrderRef);

    /**
     * Fully closes an UNPAID payment session on the gateway side, so that nobody can pay into it any more.
     *
     * <p>Returns {@code true} only when the gateway has CONFIRMED the session is dead. {@code false}
     * means "cannot guarantee" -- maybe the gateway has no cancel API, or the last call failed. The
     * caller must treat false as "the link can still bring in money" and must NOT close the order
     * (see CancelOrderUseCase).
     *
     * <p>The two gateways are NOT symmetric here, which is why this returns a boolean rather than
     * void: PayOS has POST /v2/payment-requests/{id}/cancel for exactly this, while SePay Payment
     * Gateway only has voidTransaction -- which cancels a CARD transaction that has ALREADY been
     * captured (order status = CAPTURED, payment_method = CARD, before the reconciliation cut-off),
     * that is a refund operation and not the closing of an unpaid session. There is no way to
     * cancel a SePay session early; it only dies when it expires.
     */
    boolean cancelPaymentLink(String providerOrderRef, String reason);

    // Takes raw bytes rather than an already parsed Map: many gateways sign the HMAC over the exact
    // byte string of the body, so a single parse/serialize round trip breaks the signature.
    // headers serves the gateways that put the signature in a header instead of in the body.
    CallbackVerificationResult verifyCallback(byte[] rawBody, Map<String, String> headers);
}
