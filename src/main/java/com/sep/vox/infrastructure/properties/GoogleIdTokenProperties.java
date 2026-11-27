package com.sep.vox.infrastructure.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for verifying Google ID tokens for native apps.
 *
 * <p>Only one value truly has to be set per environment: {@code allowed-audiences}. The public key set and the
 * list of {@code iss} are constants of Google itself, not something each deployment chooses, so they are here as
 * defaults instead of becoming two environment variables nobody ever changes -- same
 * thinking as {@link CorsProperties}.
 *
 * @param allowedAudiences OUR OWN client IDs accepted in {@code aud}, comma-separated through
 *                         the environment variable {@code GOOGLE_ALLOWED_AUDIENCES}.
 *                         <p>They must ALL be listed, and this is the easiest place to under-configure because each client
 *                         sends a different {@code aud}:
 *                         <ul>
 *                           <li>Flutter (Android/iOS) sends the <b>WEB</b> client ID -- that is exactly what
 *                               {@code serverClientId} of {@code google_sign_in} does, so it
 *                               equals the {@code GOOGLE_CLIENT_ID} of the web flow;</li>
 *                           <li>WPF sends its own <b>DESKTOP</b> client ID.</li>
 *                         </ul>
 *                         Setting only one value means one of the two clients is always rejected.
 * @param jwkSetUri        where to fetch Google's public keys. This key set rotates periodically; NimbusJwtDecoder
 *                         reloads it by itself so nothing more needs to be done.
 * @param allowedIssuers   Google issues {@code iss} in TWO forms for the same token -- with and without
 *                         {@code https://}. Accepting only one form randomly rejects half of
 *                         the sign-ins, and the symptom ("sometimes wrong") hides the cause.
 */
@ConfigurationProperties(prefix = "app.oauth2.google")
public record GoogleIdTokenProperties(
    List<String> allowedAudiences,
    String jwkSetUri,
    List<String> allowedIssuers
) {

    private static final String DEFAULT_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private static final List<String> DEFAULT_ISSUERS =
        List.of("https://accounts.google.com", "accounts.google.com");

    public GoogleIdTokenProperties {
        allowedAudiences = trimmed(allowedAudiences);
        jwkSetUri = jwkSetUri == null || jwkSetUri.isBlank() ? DEFAULT_JWK_SET_URI : jwkSetUri.trim();
        List<String> issuers = trimmed(allowedIssuers);
        allowedIssuers = issuers.isEmpty() ? DEFAULT_ISSUERS : issuers;
    }

    /**
     * Skips null elements, not only empty strings: an environment variable such as
     * {@code GOOGLE_ALLOWED_AUDIENCES=id-web,,id-desktop} or a YAML list with a blank entry both
     * yield a null element, and the {@code List.of} below would throw an NPE instead of skipping it. Dying at
     * startup because of a stray comma is a failure that is very hard to guess from the error message.
     */
    private static List<String> trimmed(List<String> values) {
        return values == null
            ? List.of()
            : values.stream()
                .filter(value -> value != null)
                .map(value -> value.trim())
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
