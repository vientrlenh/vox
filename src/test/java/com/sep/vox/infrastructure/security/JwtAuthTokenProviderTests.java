package com.sep.vox.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtAuthTokenProviderTests {

    private static final String SECRET = "test-access-secret-oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo";

    private JwtAuthTokenProvider jwtAuthTokenProvider;

    @BeforeEach
    void setUp() {
        jwtAuthTokenProvider = new JwtAuthTokenProvider();
        ReflectionTestUtils.setField(jwtAuthTokenProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtAuthTokenProvider, "expirationMs", 1_800_000L);
    }

    @Test
    void generateJwtToken_should_include_school_id_claim_when_user_belongs_to_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var roles = List.of("SCHOOL_ADMIN");

        var token = jwtAuthTokenProvider.generateJwtToken(userId.toString(), schoolId, "admin@example.com", roles);

        assertThat(jwtAuthTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtAuthTokenProvider.getSchoolIdFromToken(token)).isEqualTo(schoolId);
        assertThat(jwtAuthTokenProvider.getEmailFromToken(token)).isEqualTo("admin@example.com");

        var claims = claims(token);
        assertThat(claims.get("userId", String.class)).isEqualTo(userId.toString());
        assertThat(claims.get("schoolId", String.class)).isEqualTo(schoolId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("admin@example.com");
        assertThat(rolesFromClaims(claims)).containsExactly("SCHOOL_ADMIN");
    }

    @Test
    void generateJwtToken_should_omit_school_id_claim_when_user_has_no_school() {
        var userId = UUID.randomUUID();
        var roles = List.of("SYSTEM_ADMIN");

        var token = jwtAuthTokenProvider.generateJwtToken(userId.toString(), null, "sysadmin@example.com", roles);

        assertThat(jwtAuthTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtAuthTokenProvider.getSchoolIdFromToken(token)).isNull();
        assertThat(jwtAuthTokenProvider.getEmailFromToken(token)).isEqualTo("sysadmin@example.com");

        var claims = claims(token);
        assertThat(claims).doesNotContainKey("schoolId");
        assertThat(rolesFromClaims(claims)).containsExactly("SYSTEM_ADMIN");
    }

    private static Claims claims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    @SuppressWarnings("unchecked")
    private static List<String> rolesFromClaims(Claims claims) {
        return claims.get("roles", List.class);
    }

    private static SecretKey secretKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }
}
