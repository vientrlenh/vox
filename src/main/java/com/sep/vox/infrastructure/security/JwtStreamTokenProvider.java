package com.sep.vox.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    public String generateStreamToken(String userId, String candidateId, String scheduleId, String examId, String sessionId,
            List<String> streamTypes, Instant windowStart, Instant windowEnd) {
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
                .notBefore(Date.from(windowStart))
                .expiration(Date.from(windowEnd))
                .signWith(getSecretKey(secret))
                .compact();
    }

    public String generateMonitorToken(String userId, String schoolId, String examId, String monitorScope, List<String> scheduleIds, List<String> roles, Instant windowStart, Instant windowEnd) {

        // Hạn của ca thi, tách khỏi expiresAt của token
        // Giúp trả lời "bao lâu thì dữ liệu đã ghi vẫn còn đẩy lên được service streaming"
        // Dùng để đặt hạn upload credentials. Giúp nới thời gian cứu dữ liệu upload record
        var scheduleEndAt = windowEnd.getEpochSecond();

        var tokenExpiry = windowEnd.isBefore(Instant.now().plus(15, ChronoUnit.MINUTES)) ? windowEnd : Instant.now().plus(15, ChronoUnit.MINUTES);
        var claims = new HashMap<String, Object>();
        claims.put("userId", userId);
        claims.put("schoolId", schoolId);
        claims.put("examId", examId);
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
