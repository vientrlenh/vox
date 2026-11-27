package com.sep.vox.application.response.output;

public enum PaymentLinkRemoteStatus {
    PENDING,
    PROCESSING,
    UNDERPAID,
    PAID,
    CANCELLED,
    EXPIRED,
    FAILED,
    /**
     * The gateway asserts that NO session carries this code -- very different from "could not ask" (network down, 5xx,
     * rate limit exceeded), which must be thrown outward and must not be mapped to this.
     *
     * <p>It exists since the payment_records row is committed BEFORE calling the gateway
     * (CreatePaymentCheckoutUrlUseCase): if the link-creation call fails before the gateway has built a session, that
     * PENDING row stays behind and carries a code the gateway has never seen. Without telling this case apart, that attempt
     * hangs until the order expires, and the school cannot request a new link.
     *
     * <p>Safe to treat as dead: the gateway does not know the code, so there is no way money can arrive under that code.
     */
    NOT_FOUND
}