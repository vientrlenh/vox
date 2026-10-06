package com.sep.vox.infrastructure.security;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.infrastructure.properties.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor 
public class JwtAuthTokenProvider implements AuthTokenPort {

    private final JwtProperties jwtProperties;

    @Override
    public String generateToken(UUID userId, UUID schoolId, String email, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        if (schoolId != null) {
            claims.put("schoolId", schoolId.toString());
        } else {
            claims.put("schoolId", "");
        }
        claims.put("email", email);
        claims.put("roles", roles);
        return createToken(claims, userId);
    }

    private String createToken(Map<String, Object> claims, UUID userId) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + jwtProperties.expirationMs());
        SecretKey secretKey = getSignedKey();
        return Jwts.builder()
            .claims(claims)
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiresAt)
            .signWith(secretKey)
            .compact();
    }

    private SecretKey getSignedKey() {
        byte[] keyBytes = Base64.decode(jwtProperties.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String extractEmail(String token) {
        return extractClaim(token, c -> c.get("email", String.class));
    }

    @Override
    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, c -> c.getSubject());
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException ex) {
            log.debug("User ID extracted failed in claim: {}", ex.getMessage(), ex);
            throw new MalformedJwtException("Malformed token");
        }
    }

    @Override
    public UUID extractSchoolId(String token) {
        String schoolIdStr = extractClaim(token, c -> c.get("schoolId", String.class));
        if (schoolIdStr.strip().isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(schoolIdStr);
        } catch (IllegalArgumentException ex) {
            log.debug("School ID extracted failed in claim: {}", ex.getMessage(), ex);
            throw new MalformedJwtException("Malformed token");
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractClaims(String token) {
        SecretKey secretKey = getSignedKey();
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            log.info("JWT Token expired: {}", e.getMessage());
            throw new IllegalArgumentException("Token expired");
        } catch (JwtException e) {
            log.info("JWT Token error: {}", e.getMessage());
            throw new IllegalArgumentException("Malformed token");
        }
    }

    
}
