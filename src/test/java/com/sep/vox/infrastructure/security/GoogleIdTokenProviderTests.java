package com.sep.vox.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.infrastructure.properties.GoogleIdTokenProperties;

/**
 * Cửa kiểm ID token của đường đăng nhập Google từ ứng dụng native.
 *
 * <p>Trọng tâm là hai phép kiểm mà Spring KHÔNG làm hộ ở đường này, và cả hai đều thuộc loại hỏng
 * mà không có triệu chứng: mọi lần đăng nhập thật vẫn chạy đúng như cũ, chỉ có cửa là mở.
 */
class GoogleIdTokenProviderTests {

    private static final String WEB_CLIENT_ID = "111-web.apps.googleusercontent.com";
    private static final String DESKTOP_CLIENT_ID = "222-desktop.apps.googleusercontent.com";

    /**
     * The most important check of the whole class.
     *
     * <p>The token in this test is a REAL token in every other sense -- signed by Google, issued by
     * Google, not expired -- it differs only in that it was issued to someone else's application.
     * Without the {@code aud} check, the signature, {@code iss} and expiry all say "valid", and
     * anyone with any Google app could log into any account just by knowing the email.
     */
    @Test
    void should_reject_a_token_minted_for_someone_elses_google_app() {
        OAuth2TokenValidator<Jwt> validator = GoogleIdTokenProvider.audienceValidator(List.of(WEB_CLIENT_ID, DESKTOP_CLIENT_ID));

        OAuth2TokenValidatorResult result = validator.validate(jwt().audience(List.of("999-attacker.apps.googleusercontent.com")).build());

        assertThat(result.hasErrors()).isTrue();
    }

    /**
     * Both clients must pass: Flutter sends the aud of the WEB client, WPF sends the aud of the
     * DESKTOP client.
     */
    @Test
    void should_accept_every_configured_client_id() {
        OAuth2TokenValidator<Jwt> validator = GoogleIdTokenProvider.audienceValidator(List.of(WEB_CLIENT_ID, DESKTOP_CLIENT_ID));

        assertThat(validator.validate(jwt().audience(List.of(WEB_CLIENT_ID)).build()).hasErrors()).isFalse();
        assertThat(validator.validate(jwt().audience(List.of(DESKTOP_CLIENT_ID)).build()).hasErrors()).isFalse();
    }

    /** Not configured means no client is valid -- NOT "every client is valid". */
    @Test
    void should_reject_everything_when_no_audience_is_configured() {
        OAuth2TokenValidator<Jwt> validator = GoogleIdTokenProvider.audienceValidator(List.of());

        assertThat(validator.validate(jwt().audience(List.of(WEB_CLIENT_ID)).build()).hasErrors()).isTrue();
    }

    /**
     * Google issues {@code iss} in TWO forms for the same token, with and without {@code https://}.
     * Accepting only one form randomly rejects a share of real logins -- and the symptom
     * ("sometimes wrong") hides the cause.
     */
    @Test
    void should_accept_both_issuer_spellings_google_actually_uses() {
        OAuth2TokenValidator<Jwt> validator = GoogleIdTokenProvider.issuerValidator(
            List.of("https://accounts.google.com", "accounts.google.com"));

        assertThat(validator.validate(jwt().issuer("https://accounts.google.com").build()).hasErrors()).isFalse();
        assertThat(validator.validate(jwt().issuer("accounts.google.com").build()).hasErrors()).isFalse();
    }

    @Test
    void should_reject_a_token_from_another_issuer() {
        OAuth2TokenValidator<Jwt> validator = GoogleIdTokenProvider.issuerValidator(List.of("https://accounts.google.com"));

        assertThat(validator.validate(jwt().issuer("https://accounts.evil.example").build()).hasErrors()).isTrue();
    }

    /**
     * If aud is not configured, reject IMMEDIATELY, even before touching Google.
     *
     * <p>This is not a speed optimization: if the token were allowed to continue, the aud check
     * against an empty list would still fail, but the operator would read "invalid token" for a
     * completely valid token. This is a CONFIGURATION error and the message must say exactly that.
     */
    @Test
    void should_fail_loudly_when_the_feature_is_not_configured() {
        GoogleIdTokenProvider provider = new GoogleIdTokenProvider(properties(List.of()));

        assertThatThrownBy(() -> provider.verify("bat-ky-chuoi-nao"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("chưa được cấu hình");
    }

    /** An empty token is not worth a network round trip to Google. */
    @Test
    void should_reject_a_blank_token_without_calling_google() {
        GoogleIdTokenProvider provider = new GoogleIdTokenProvider(properties(List.of(WEB_CLIENT_ID)));

        assertThatThrownBy(() -> provider.verify("  ")).isInstanceOf(UnauthorizedException.class);
    }

    /**
     * iss/jwk-set-uri are Google's constants, not something each environment sets for itself.
     *
     * <p>The input list is deliberately dirty: {@code Arrays.asList} rather than {@code List.of}
     * because it ALLOWS null, exactly what an environment variable with a stray comma produces.
     */
    @Test
    void should_default_the_google_constants_so_only_audiences_need_configuring() {
        GoogleIdTokenProperties properties = new GoogleIdTokenProperties(
            Arrays.asList(" " + WEB_CLIENT_ID + " ", "", null), null, null);

        assertThat(properties.allowedAudiences()).containsExactly(WEB_CLIENT_ID);
        assertThat(properties.jwkSetUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
        assertThat(properties.allowedIssuers())
            .containsExactly("https://accounts.google.com", "accounts.google.com");
    }

    private static GoogleIdTokenProperties properties(List<String> audiences) {
        return new GoogleIdTokenProperties(audiences, null, null);
    }

    /**
     * Builds the {@link Jwt} with the builder instead of signing for real: the two validators under
     * test only read claims, so running an RSA key pair and a JWK set locally just to pin a string
     * comparison is wasted effort.
     */
    private static Jwt.Builder jwt() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("google-sub-1")
            .issuer("https://accounts.google.com")
            .audience(List.of(WEB_CLIENT_ID))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600));
    }
}
