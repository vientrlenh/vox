package com.sep.vox.infrastructure.security;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.AuthTokenPort;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider implements AuthTokenPort {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;


    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Override
    public String generateJwtToken(String userId, @Nullable UUID schoolId, String email, List<String> roles) {
        var claims = new HashMap<String, Object>();
        claims.put("userId", userId);
        if (schoolId != null) {
            claims.put("schoolId", schoolId.toString());
        }
        claims.put("email", email);
        claims.put("roles", roles);
        return createToken(userId, claims, expirationMs, secret);
    }

    @Override
    public String getEmailFromToken(String token) {
        var claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    @Override
    public UUID getUserIdFromToken(String token) {
       var claims = getClaimsFromToken(token);
       var userIdStr = claims.get("userId", String.class);
       return UUID.fromString(userIdStr);
    }

    @Override
    public @Nullable UUID getSchoolIdFromToken(String token) {
       var claims = getClaimsFromToken(token);
       var schoolIdStr = claims.get("schoolId", String.class);
       return schoolIdStr == null ? null : UUID.fromString(schoolIdStr);
    }


    private String createToken(String userId, Map<String, Object> claims, long expirationMs, String keyBytes) {
        var now = new Date();
        var expiration = new Date(now.getTime() + expirationMs);
        var secretKey = getSecretKey(keyBytes);
        return buildToken(userId, claims, now, expiration, secretKey);
    }

    private String buildToken(String userId, Map<String, Object> claims, Date issue, Date expiration, SecretKey key) {
        return Jwts.builder()
            .claims(claims)
            .subject(userId)
            .issuedAt(issue)
            .expiration(expiration)
            .signWith(key)
            .compact();
    }

    private SecretKey getSecretKey(String keyBytes) {
        return Keys.hmacShaKeyFor(keyBytes.getBytes());
    }

    private Claims getClaimsFromToken(String token) {
        var secretKey = getSecretKey(secret);
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            LOGGER.info("JWT Token expired: {}", e.getMessage());
            throw new IllegalArgumentException("Token đã hết hạn");
        } catch (JwtException e) {
            LOGGER.info("JWT Token error: {}", e.getMessage());
            throw new IllegalArgumentException("Token không hợp lệ");
        }
    }

    
}
