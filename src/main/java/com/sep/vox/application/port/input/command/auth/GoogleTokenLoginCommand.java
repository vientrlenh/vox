package com.sep.vox.application.port.input.command.auth;

/**
 * Logs in with a Google ID token obtained NATIVELY on the user's device.
 *
 * <p>Different from {@link OAuth2LoginCommand} in that all the user information is still LOCKED inside
 * {@code idToken} and has not been verified. {@code OAuth2LoginCommand} is what comes out AFTER verification --
 * there the email/sub is already verified fact, while here it is only a string sent up by the client.
 * Two separate records exist precisely so that no path can mistake one for the other.
 */
public record GoogleTokenLoginCommand(
    String idToken,
    String ipAddress,
    String userAgent,
    ClientDeviceCommand device
) {
}
