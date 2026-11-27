package com.sep.vox.application.port.output;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.response.output.VerifiedGoogleIdentity;

/**
 * Verifies an ID token that Google issued to a NATIVE application (Flutter, WPF).
 *
 * <p>Why this path exists next to the web redirect flow: the web flow is handled end to end by
 * Spring Security -- it exchanges the code for a token, checks the signature, and finishes with a
 * redirect to EXACTLY ONE preconfigured URL ({@code app.frontend.oauth2-url}). Native apps have
 * nowhere to receive that redirect, so they obtain the ID token themselves with an SDK/PKCE on the
 * user's device and submit it here.
 *
 * <p><b>This entry point replaces all the protection that Spring Security provides on the other
 * path</b>, so the implementation must check all four things -- missing any one is a real login hole:
 * <ul>
 *   <li>the signature, against Google's public key set;</li>
 *   <li>{@code iss} belongs to Google;</li>
 *   <li>{@code aud} is in OUR OWN list of client IDs -- the easiest check to forget and also the
 *       deadliest: a perfectly valid ID token issued by Google to ANOTHER APPLICATION still passes
 *       the other three checks. Without this check, anyone with any Google app could log in to
 *       every account in the system;</li>
 *   <li>the validity period ({@code exp}/{@code iat}).</li>
 * </ul>
 *
 * <p>Does NOT check permissions and does not look up the user: this port only answers "does Google
 * really say that the holder of this token is that email address". Whether that email is an active
 * user of the system is the question of {@code OAuth2LoginUseCase}.
 */
public interface GoogleIdTokenVerifierPort {

    /**
     * @param idToken the raw ID token (JWT) that the client obtained from Google
     * @return the verified identity
     * @throws UnauthorizedException when the token has a wrong signature, wrong
     *         {@code iss}/{@code aud}, has expired, or when the system has no list of accepted
     *         {@code aud} values configured
     */
    VerifiedGoogleIdentity verify(String idToken);
}
