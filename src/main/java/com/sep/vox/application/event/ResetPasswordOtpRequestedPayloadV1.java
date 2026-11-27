package com.sep.vox.application.event;

/**
 * A request to send the OTP that resets a password.
 *
 * <p>The payload does NOT carry the OTP code -- see {@code EventTypeConstant.RESET_PASSWORD_OTP_REQUESTED}.
 * The code is generated in the consumer right before the mail is sent, so it never exists in
 * {@code outboxes.payload} or in the Kafka topic.
 */
public record ResetPasswordOtpRequestedPayloadV1(
    String to
) {
}
