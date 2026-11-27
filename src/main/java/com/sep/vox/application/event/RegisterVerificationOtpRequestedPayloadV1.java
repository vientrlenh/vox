package com.sep.vox.application.event;

/**
 * A request to send the OTP that verifies a school registration.
 *
 * <p>The payload does NOT carry the OTP code, for the same reason as
 * {@link ResetPasswordOtpRequestedPayloadV1}. The consumer generates the code, writes its hash
 * into the registration cache row (already created by the use case with an empty
 * {@code otpHash}) and only then sends the mail.
 */
public record RegisterVerificationOtpRequestedPayloadV1(
    String to
) {
}
