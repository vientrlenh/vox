package com.sep.vox.application.response.input.payment;

public record PaymentCallbackResponse(
    CallbackOutcome outcome
) {
        /** The outcome, so the REST layer can pick the message to return. */
    public enum CallbackOutcome {
        /** The attempt has been settled (success or failure). */
        SETTLED,
        /** The signature is valid but matches no attempt -- e.g. a fixed test payload from the gateway. */
        UNKNOWN_PAYMENT,
        /** This attempt was already settled earlier -- a repeated callback, nothing more to do. */
        ALREADY_SETTLED,
        /**
         * The gateway reports it HAS BEEN PAID for an attempt we recorded as failed: the money really arrived but no row is left
         * to receive it. Kept apart from ALREADY_SETTLED because the two cases are opposite in severity --
         * one is a harmless repeated callback, the other is the school's money hanging in limbo.
         */
        PAID_AFTER_WRITE_OFF,
        /** The reported amount differs from the attempt: stop, a person must look. */
        AMOUNT_MISMATCH,
        /** The status is not a final status yet (or could not be mapped) -- nothing settled. */
        NOT_FINAL
    }

    public static PaymentCallbackResponse toResponse(CallbackOutcome outcome) {
        return new PaymentCallbackResponse(outcome);
    }
}
