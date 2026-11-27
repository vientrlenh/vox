package com.sep.vox.application.response.output;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything the FE needs to send the user to the payment page, normalized for every gateway.
 *
 * @param action        redirect or submit a form -- see {@link CheckoutAction}
 * @param actionUrl     the redirect target (REDIRECT) or the form's action attribute (FORM_POST)
 * @param paymentLinkId the link identifier on the gateway side, for cross-checking against the dashboard; null for a gateway
 *                      without this concept (SePay PG identifies the order by order_invoice_number)
 * @param fields        the form's hidden fields, empty when action is REDIRECT. The key order is part of
 *                      the contract and not a presentation detail: SePay signs the string joined in exactly that
 *                      order, so the form must POST in this same order. Contains the
 *                      signature so it must not be written to logs
 * @param qrCode        the VietQR string for the FE to draw the code itself, null for a gateway that returns no QR
 * @param transfer      the manual transfer details that go with the QR code, null when there is no QR
 */
public record PaymentCheckoutResult(
    CheckoutAction action,
    String actionUrl,
    String paymentLinkId,
    Map<String, String> fields,
    String qrCode,
    BankTransferDetails transfer
) {

    public static PaymentCheckoutResult redirect(String checkoutUrl, String paymentLinkId) {
        return new PaymentCheckoutResult(
            CheckoutAction.REDIRECT, checkoutUrl, paymentLinkId, Map.of(), null, null);
    }

    /**
     * A gateway that both returns a QR code and has its own checkout page: show the code in the app, keep the URL as an
     * escape hatch. Without a QR, do not use this factory -- use {@link #redirect} so the FE does not have to guess.
     */
    public static PaymentCheckoutResult qr(
            String qrCode, BankTransferDetails transfer, String checkoutUrl, String paymentLinkId) {
        return new PaymentCheckoutResult(
            CheckoutAction.QR, checkoutUrl, paymentLinkId, Map.of(), qrCode, transfer);
    }

    // LinkedHashMap and not Map.copyOf: Map.copyOf returns a map with no defined order, and here the
    // order is exactly what decides whether the signature matches.
    public static PaymentCheckoutResult formPost(String actionUrl, Map<String, String> fields) {
        return new PaymentCheckoutResult(
            CheckoutAction.FORM_POST, actionUrl, null,
            Collections.unmodifiableMap(new LinkedHashMap<>(fields)), null, null);
    }
}
