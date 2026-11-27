package com.sep.vox.infrastructure.security;

import java.net.URL;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.GoogleIdTokenVerifierPort;
import com.sep.vox.application.response.output.VerifiedGoogleIdentity;
import com.sep.vox.infrastructure.properties.GoogleIdTokenProperties;

/**
 * Verifies a Google ID token with {@link NimbusJwtDecoder}, not with Google's library.
 *
 * <p>Why Nimbus: it is already on the classpath through {@code spring-boot-starter-oauth2-client}
 * -- the same library that the web redirect flow itself uses to verify tokens. Google's version
 * ({@code GoogleIdTokenVerifier}) only arrives here through a BRIDGING dependency of
 * {@code firebase-admin}, meaning something that could disappear in a firebase version upgrade
 * without anyone reading the changelog -- and what would disappear then is the authentication gate.
 *
 * <p>Google's public key set rotates periodically; {@code NimbusJwtDecoder} reloads and caches it by itself so
 * nothing more needs to be done here. The decoder is built ONCE in the constructor and not on each call:
 * rebuilding on every request means reloading the JWK set on every login, turning every button press into
 * a network round trip out to the internet.
 */
@Component
@Slf4j
public class GoogleIdTokenProvider implements GoogleIdTokenVerifierPort {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_EMAIL_VERIFIED = "email_verified";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_PICTURE = "picture";

    private final JwtDecoder jwtDecoder;
    private final List<String> allowedAudiences;

    public GoogleIdTokenProvider(GoogleIdTokenProperties properties) {
        this.allowedAudiences = properties.allowedAudiences();

        // Warn instead of throwing: this is an ADDED feature, and every deployment currently running has not
        // set this variable. Dying at startup would take down the whole system because of a login path nobody has
        // used yet -- unlike CORS or the admin account, which are always required so fail-fast is right.
        // The real rejection lives in verify(), where a real person is trying to log in and can be told about the error.
        if (allowedAudiences.isEmpty()) {
            log.warn(
                "app.oauth2.google.allowed-audiences is empty -- Google sign-in from native apps "
                    + "(Flutter/WPF) will be rejected entirely. Set the environment variable "
                    + "GOOGLE_ALLOWED_AUDIENCES to include the WEB client ID (Flutter sends this through "
                    + "serverClientId) and the DESKTOP client ID (WPF). The web Google sign-in flow "
                    + "is NOT affected.");
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            issuerValidator(properties.allowedIssuers()),
            audienceValidator(allowedAudiences)
        ));
        this.jwtDecoder = decoder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Every Nimbus error is mapped to ONE generic message, deliberately. {@link JwtException} can tell
     * "wrong signature", "expired", "wrong aud" apart -- but the person who hits the error here is the one HOLDING the token,
     * and telling them exactly which check failed is giving directions to probe step by step. The details go into the
     * log, where operators can read them and attackers cannot.
     */
    @Override
    public VerifiedGoogleIdentity verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Google sign-in failed");
        }

        if (allowedAudiences.isEmpty()) {
            log.error(
                "Rejected Google sign-in: app.oauth2.google.allowed-audiences is not configured.");
            throw new UnauthorizedException("Google sign-in is not configured on the system");
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException e) {
            log.warn("Invalid Google ID token: {}", e.getMessage());
            throw new UnauthorizedException("Google sign-in failed");
        }

        String email = jwt.getClaimAsString(CLAIM_EMAIL);
        if (email == null || email.isBlank()) {
            // The token is valid but carries no email scope, so there is nothing to look the user up with.
            log.warn("Valid Google ID token but missing the email claim -- is the client missing a scope?");
            throw new UnauthorizedException("Google sign-in failed");
        }

        return new VerifiedGoogleIdentity(
            jwt.getSubject(),
            email,
            emailVerified(jwt),
            jwt.getClaimAsString(CLAIM_NAME),
            jwt.getClaimAsString(CLAIM_PICTURE)
        );
    }

    /**
     * {@code email_verified} arrives on the wire sometimes as a JSON boolean, sometimes as the string "true".
     *
     * <p>This is not a defensive guess: it is a real difference between Google's token-issuing paths,
     * and {@code getClaimAsBoolean} on the string "true" returns null. Reading it that way and treating null as
     * false would reject perfectly valid accounts -- a bug that only appears for some
     * users, which is the hardest kind to reproduce.
     */
    private static boolean emailVerified(Jwt jwt) {
        Object claim = jwt.getClaim(CLAIM_EMAIL_VERIFIED);
        if (claim instanceof Boolean verified) {
            return verified;
        }
        return claim instanceof String verified && Boolean.parseBoolean(verified);
    }

    /**
     * Google issues {@code iss} in two forms for the same token, with and without {@code https://}.
     *
     * <p>So Spring's {@code JwtIssuerValidator} cannot be used: it accepts exactly ONE value, and
     * whichever form is chosen it rejects half of the real tokens.
     *
     * <p>Reads the raw claim with {@code getClaimAsString} and NOT with {@code jwt.getIssuer()}.
     * {@code getIssuer()} returns a {@link URL} so it throws
     * {@code IllegalArgumentException: Unable to convert claim 'iss' ... to URL} on exactly the form
     * {@code accounts.google.com} (no scheme) that this function exists to accept -- i.e. the
     * check would blow up in the very case it exists to handle. {@code getClaimAsString} does not touch URL so it is
     * correct for both forms.
     *
     * <p>Package-private (not private) so tests can call it directly: building a {@link Jwt} with a builder is much cheaper
     * than really signing a token and hosting a JWK set just to pin down a string comparison.
     */
    static OAuth2TokenValidator<Jwt> issuerValidator(List<String> allowedIssuers) {
        return jwt -> allowedIssuers.contains(jwt.getClaimAsString(JwtClaimNames.ISS))
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_issuer", "The ID token was not issued by Google", null));
    }

    /**
     * The MOST IMPORTANT check of this class.
     *
     * <p>The other three checks (signature, iss, expiry) only answer "did Google really issue this token".
     * They do NOT answer "which APP did Google issue it for". Without the aud check, a completely genuine ID
     * token that Google issued to ANY app of ANY developer would pass this gate
     * -- meaning anyone could sign in to any account, as long as they know the
     * email. This is the classic hole of the "send the ID token to the server" style of sign-in.
     *
     * <p>Package-private, same reason as {@link #issuerValidator(List)} -- and even more so here: this is
     * a check that an accidental refactor can break without causing any symptom. Every real
     * sign-in still works correctly; only the gate is wide open.
     */
    static OAuth2TokenValidator<Jwt> audienceValidator(List<String> allowedAudiences) {
        return jwt -> jwt.getAudience() != null
                && jwt.getAudience().stream().anyMatch(allowedAudiences::contains)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_audience", "The ID token is not intended for this application", null));
    }
}
