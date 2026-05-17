package com.sep.vox.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.domain.model.session.Session;
import com.sep.vox.domain.repository.SessionRepository;
import com.sep.vox.infrastructure.exception.InfrastructureException;

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
    public boolean compareMatchesToken(String raw, UUID requestedUserId) {
        var hashedFromRaw = hashAndGetRefreshToken(raw);
        var actualHashed = sessionRepository.findByUserIdAndRefreshTokenHash(requestedUserId, hashedFromRaw)
            .orElse(null);
        if (actualHashed == null || !hashedFromRaw.equals(actualHashed.getRefreshTokenHash())) 
            return false;
        return true;
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


    
}
