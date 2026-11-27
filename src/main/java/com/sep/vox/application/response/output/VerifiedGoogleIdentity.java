package com.sep.vox.application.response.output;

/**
 * A verified Google identity -- only the fields that the login flow actually uses.
 *
 * <p>This is the result of a cryptographic VERIFICATION, not a blob of JSON sent up by the client. Every
 * value here exists only after the signature, {@code iss}, {@code aud} and the validity period of the ID token have all
 * passed -- see {@code GoogleIdTokenVerifierPort}. So the consumer does not have to ask "can it be trusted yet",
 * and there is no path for an unverified field to slip in here.
 *
 * @param subject       Google's {@code sub} -- the stable identifier of the account, unchanged even when
 *                      the user changes their email. Goes straight into {@code OAuth2LoginCommand.providerUserId}.
 * @param email         the verified email; this is the key used to look up the user in the system
 * @param emailVerified Google does NOT guarantee every ID token has a verified email (a Workspace account
 *                      configured oddly, or a token issued for a scope without email), so this field
 *                      is carried along instead of assumed -- the caller must reject it itself, just as
 *                      {@code CustomOidcUserService} does for the web path.
 * @param fullName      may be null: {@code name} is only present when the token was issued with the {@code profile} scope
 * @param avatarUrl     may be null, for the same reason as {@code fullName}
 */
public record VerifiedGoogleIdentity(
    String subject,
    String email,
    boolean emailVerified,
    String fullName,
    String avatarUrl
) {
}
