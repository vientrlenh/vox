package com.sep.vox.application.response.output;

/**
 * How the FE must open the gateway's payment page. This could not be expressed before: the old field
 * {@code method} carried the gateway name ("PAYOS"/"SEPAY"), so the FE had to hardcode "if SEPAY then
 * submit a form" -- meaning every new gateway required changing the FE.
 */
public enum CheckoutAction {

    /** Redirects the browser to {@code actionUrl} (PayOS returns its own checkout link for each order). */
    REDIRECT,

    /**
     * Shows the VietQR code RIGHT INSIDE the app, without leaving the page. The user scans it with a banking app, or
     * transfers by hand using {@code transfer}.
     *
     * <p>Differs from REDIRECT in one point that decides how the FE must be written: after scanning, NO event brings the
     * user back, so the page must poll the order status itself from the moment the code is shown. {@code
     * actionUrl} is still there (the gateway's checkout page) and should be kept as an escape hatch for whoever wants that page.
     *
     * <p>Only a gateway that RETURNS a QR string can use it. SePay PG has none -- it remains FORM_POST.
     */
    QR,

    /**
     * Builds a hidden form with all the {@code fields} and POSTs it to {@code actionUrl}. SePay PG works
     * this way: the checkout URL is fixed, and the order is described by fields with an HMAC signature.
     */
    FORM_POST,

    /**
     * Nothing more to pay -- the invoice was settled PAID immediately (amountDue = 0 after offsetting the
     * unused days, see SubscriptionUpgradePolicyService), the FE does not need to go to any gateway.
     */
    NONE
}
