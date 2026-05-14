package com.sep.vox.infrastructure.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.infrastructure.exception.InfrastructureException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider implements AuthTokenPort {

    @Value("${jwt.access-secret}")
    private String accessSecret;

    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String ACCESS_TYPE = "ACCESS";
    private static final String REFRESH_TYPE = "REFRESH";

    @Override
    public String generateJwtToken(String userId, String email, String role, String type) {
        switch (type) {
            case ACCESS_TYPE:
                return generateAccessToken(userId, email, role);
            case REFRESH_TYPE:
                return generateRefreshToken(userId, email);
            default:
                LOGGER.error("Invalid JWT Token type: {} at generateJwtToken", type);
                throw new InfrastructureException("Loại JWT Token không hợp lệ");
        }
    }

    @Override
    public String getEmailFromToken(String token, String type) {
        var claims = getClaimsFromToken(token, type);
        return claims.get("email", String.class);
    }

    @Override
    public UUID getUserIdFromToken(String token, String type) {
       var claims = getClaimsFromToken(token, type);
       var userIdStr = claims.get("userId", String.class);
       return UUID.fromString(userIdStr);
    }

    private String generateAccessToken(String userId, String email, String role) {
        var claims = new HashMap<String, Object>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);
        return createToken(userId, claims, accessExpirationMs, accessSecret);
    }

    private String generateRefreshToken(String userId, String email) {
        var claims = new HashMap<String, Object>();
        claims.put("userId", userId);
        claims.put("email", email);
        return createToken(userId, claims, refreshExpirationMs, refreshSecret);
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


    private Claims getClaimsFromToken(String token, String type) {
        SecretKey secretKey;
        switch (type) {
            case ACCESS_TYPE:
                secretKey = getSecretKey(accessSecret);
                break;
            case REFRESH_TYPE:
                secretKey = getSecretKey(refreshSecret);
                break;
            default:
                LOGGER.error("Invalid JWT Token type: {} at getClaimsFromToken", type);
                throw new InfrastructureException("Loại JWT Token không hợp lệ");
        }
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
