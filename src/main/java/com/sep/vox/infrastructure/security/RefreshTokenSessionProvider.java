package com.sep.vox.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.domain.model.session.Session;
import com.sep.vox.domain.repository.SessionRepository;
import com.sep.vox.infrastructure.exception.InfrastructureException;

@Component
public class RefreshTokenSessionProvider implements SessionManagerPort {

    private final SessionRepository sessionRepository;

    public RefreshTokenSessionProvider(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SR = new SecureRandom();
    private static final int TOKEN_LENGTH = 20;
    private static final String HASH_METHOD = "SHA-512";
    private static final int DAY_TILL_EXPIRE = 3;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenSessionProvider.class);

    @Override
    public String setSessionAndGetRefreshTokenWhenLogin(UUID userId) {
        var refreshToken = getRefreshToken();
        var hashedToken = hashAndGetRefreshToken(refreshToken);
        var issuedAt = OffsetDateTime.now();
        var expiredAt = issuedAt.plusDays(DAY_TILL_EXPIRE);

        var session = new Session(
            userId, 
            hashedToken, 
            issuedAt, 
            expiredAt, 
            null, 
            null
        );
        sessionRepository.save(session);
        return refreshToken;
    }

    @Override
    public String setSessionAndGetRefreshTokenWhenRefresh(UUID userId, String token) {
        var hashedFromRaw = hashAndGetRefreshToken(token);
        var activeSession = sessionRepository.findByUserIdAndRefreshTokenHash(userId, hashedFromRaw)
            .orElse(null);
        var matches = compareMatchesToken(hashedFromRaw, activeSession);
        if (!matches) {
            LOGGER.info("Requested refresh token does not match: {}, token value: {}", userId, token);
            return null;
        }
        var now = OffsetDateTime.now();
        if (activeSession.getExpiredAt().isBefore(now) || activeSession.getRevokedAt() != null) {
            LOGGER.info("Requested refresh token has been expired or revoked: {}", token);
            return null;
        }
        var newToken = getRefreshToken();
        var newHashedToken = hashAndGetRefreshToken(newToken);
        var expiredAt = now.plusDays(DAY_TILL_EXPIRE);
        var newSession = new Session(
            userId, 
            newHashedToken, 
            now, 
            expiredAt, 
            null, 
            activeSession.getId()
        );
        
        activeSession.setRevokedAt(now);
        sessionRepository.save(newSession);
        sessionRepository.save(activeSession);
        return newToken;
    }

    private String hashAndGetRefreshToken(String token) {
        try {
            var md = MessageDigest.getInstance(HASH_METHOD);
            var hashByte = md.digest(token.getBytes(StandardCharsets.UTF_8));

            var sb = new StringBuilder();
            for (byte b: hashByte) {
                var hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) 
                    sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new InfrastructureException("SHA-512 algorithm not found: " + e.getMessage());
        }
    }

    private String getRefreshToken() {
        return SR.ints(TOKEN_LENGTH, 0, ALPHA_NUMERIC.length())
            .mapToObj(ALPHA_NUMERIC::charAt)
            .map(Object::toString)
            .collect(Collectors.joining());
    }

    private boolean compareMatchesToken(String hashedFromRaw, Session session) {
        if (session == null || !hashedFromRaw.equals(session.getRefreshTokenHash())) 
            return false;
        return true;
    }
    
}
