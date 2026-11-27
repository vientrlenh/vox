package com.sep.vox.application.response.input.payment;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.response.output.BankTransferDetails;
import com.sep.vox.application.response.output.CheckoutAction;
import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.domain.model.payment.PaymentProvider;

/**
 * Everything the FE needs to send the school to the payment page.
 *
 * <p>The FE looks at {@code action} to know what to do, NOT at {@code provider}: provider is only for display
 * ("Pay via PayOS") and reconciliation. Branching on the gateway name would mean adding a new gateway requires changing the FE --
 * exactly what {@link CheckoutAction} exists to avoid.
 *
 * @param orderId          the order being paid -- NOT invoiceId as in the old PaymentLinkDto: the invoice
 *                         is now only issued after the money arrives, and no invoice exists when the link is issued
 * @param paymentId        the corresponding payment attempt, so the FE can ask for its status again
 * @param providerOrderRef the order code on the gateway side, as a string and not a Long: a numeric orderCode is
 *                         PayOS's own convention
 * @param fields           the hidden fields to POST when {@code action} is FORM_POST, empty for REDIRECT.
 *                         The key order is part of the contract (SePay signs the string joined in exactly
 *                         that order). CONTAINS THE SIGNATURE so it must not be logged or stored in the DB
 * @param qrCode           the VietQR string for the FE to draw the code itself, null when the action is not QR
 * @param transfer         the manual transfer details that go with the QR code, null when there is no QR
 */
public record PaymentCheckoutResponse(
    UUID orderId,
    UUID paymentId,
    String providerOrderRef,
    String provider,
    String action,
    String checkoutUrl,
    Map<String, String> fields,
    String qrCode,
    BankTransferDetails transfer
) {

    public static PaymentCheckoutResponse from(
            UUID orderId, UUID paymentId, String providerOrderRef, PaymentProvider provider,
            PaymentCheckoutResult result) {
        return new PaymentCheckoutResponse(
            orderId,
            paymentId,
            providerOrderRef,
            provider.name(),
            result.action().name(),
            result.actionUrl(),
            result.fields(),
            result.qrCode(),
            result.transfer()
        );
    }

    /**
     * Rebuilt from a PENDING attempt of which we only still hold the checkout_url. Always REDIRECT: a FORM_POST
     * gateway cannot be recreated here because the field set carries a signature, and the signature is deliberately not stored.
     */
    public static PaymentCheckoutResponse redirectTo(
            UUID orderId, UUID paymentId, String providerOrderRef, PaymentProvider provider, String checkoutUrl) {
        return new PaymentCheckoutResponse(
            orderId, paymentId, providerOrderRef, provider.name(), "REDIRECT", checkoutUrl, Map.of(), null, null);
    }

    /**
     * Rebuilt from a PENDING attempt of a QR gateway, from the payload stored in payment_records.
     *
     * <p>This is why the QR string must be STORED and not only checkout_url: a status query returns only the
     * status, the gateway does not reissue the code. Without storing it, a school that leaves the page and comes back would no longer see its
     * own code -- and leaving the page to open the banking app is the normal path here.
     */
    public static PaymentCheckoutResponse qrFor(
            UUID orderId, UUID paymentId, String providerOrderRef, PaymentProvider provider,
            String qrCode, BankTransferDetails transfer, String checkoutUrl) {
        return new PaymentCheckoutResponse(
            orderId, paymentId, providerOrderRef, provider.name(), "QR", checkoutUrl, Map.of(), qrCode, transfer);
    }
}
