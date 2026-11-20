package com.sep.vox.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.StreamTokenProvider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtStreamTokenProvider implements StreamTokenProvider {

    @Value("${jwt.stream.secret}")
    private String secret;

    @Override
    public String generateStreamToken(String userId, String candidateId, String scheduleId, String assessmentId, String sessionId,
            List<String> streamTypes, Instant windowStart, Instant windowEnd) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("candidateId", candidateId);
        claims.put("scheduleId", scheduleId);
        claims.put("assessmentId", assessmentId);
        claims.put("sessionId", sessionId);
        claims.put("streamTypes", streamTypes);
        claims.put("tokenUse", "stream");

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(new Date())
                .notBefore(Date.from(windowStart))
                .expiration(Date.from(windowEnd))
                .signWith(getSecretKey(secret))
                .compact();
    }

    public String generateMonitorToken(String userId, String schoolId, String assessmentId, String monitorScope, List<String> scheduleIds, List<String> roles, Instant windowStart, Instant windowEnd) {

        // The assessment shift's deadline, separate from the token's expiresAt.
        // Helps answer "how long after can data already recorded still be pushed to the streaming service".
        // Used to set the expiry of the upload credentials. Helps extend the time available to rescue the upload record data.
        long scheduleEndAt = windowEnd.getEpochSecond();

        Instant tokenExpiry = windowEnd.isBefore(Instant.now().plus(15, ChronoUnit.MINUTES)) ? windowEnd : Instant.now().plus(15, ChronoUnit.MINUTES);
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("userId", userId);
        claims.put("schoolId", schoolId);
        claims.put("assessmentId", assessmentId);
        claims.put("monitorScope", monitorScope);
        claims.put("scheduleIds", scheduleIds);
        claims.put("roles", roles);
        claims.put("tokenUse", "monitor");
        claims.put("scheduleEndAt", scheduleEndAt);
        return Jwts.builder()
            .claims(claims)
            .subject(userId)
            .id(UUID.randomUUID().toString())
            .issuedAt(new Date())
            .notBefore(Date.from(windowStart))
            .expiration(Date.from(tokenExpiry))
            .signWith(getSecretKey(secret))
            .compact();
    }

    private SecretKey getSecretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
}
