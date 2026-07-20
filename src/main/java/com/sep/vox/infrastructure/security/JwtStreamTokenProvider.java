package com.sep.vox.infrastructure.security;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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
    public String generateStreamToken(String userId, String candidateId, String scheduleId, String examId, String sessionId,
            List<String> streamTypes, OffsetDateTime windowStart, OffsetDateTime windowEnd) {
        var claims = new HashMap<String, Object>();
        claims.put("candidateId", candidateId);
        claims.put("scheduleId", scheduleId);
        claims.put("examId", examId);
        claims.put("sessionId", sessionId);
        claims.put("streamTypes", streamTypes);
        claims.put("tokenUse", "stream");

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(new Date())
                .notBefore(Date.from(windowStart.toInstant()))
                .expiration(Date.from(windowEnd.toInstant()))
                .signWith(getSecretKey(secret))
                .compact();
    }

    public String generateMonitorToken(String userId, List<String> sessionIds, List<String> scheduleIds, String examId, List<String> roles, OffsetDateTime windowStart, OffsetDateTime windowEnd) {
        var claims = new HashMap<String, Object>();
        claims.put("userId", userId);
        claims.put("sessionIds", sessionIds);
        claims.put("scheduleIds", scheduleIds);
        claims.put("examId", examId);
        claims.put("roles", roles);
        claims.put("tokenUse", "monitor");
        return Jwts.builder()
            .claims(claims)
            .subject(userId)
            .issuedAt(new Date())
            .notBefore(Date.from(windowStart.toInstant()))
            .expiration(Date.from(windowEnd.toInstant()))
            .signWith(getSecretKey(secret))
            .compact();
    }

    private SecretKey getSecretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
}
