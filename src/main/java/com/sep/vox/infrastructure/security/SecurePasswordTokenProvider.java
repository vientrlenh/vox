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

import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.infrastructure.exception.InfrastructureException;

public class SecurePasswordTokenProvider implements PasswordSetUpTokenPort {



    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SR = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;
    private static final String HASH_METHOD = "SHA-512";
    private static final int DAY_TILL_EXPIRE = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurePasswordTokenProvider.class);

    @Override
    public String generatePasswordSetUpToken() {
        var token = SR.ints(TOKEN_LENGTH, 0, ALPHA_NUMERIC.length())
        .mapToObj(ALPHA_NUMERIC::charAt)
        .map(Object::toString)
        .collect(Collectors.joining());
        return null;
    }

    private void hashAndSave(UUID userId, String token) {
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
            var hashed = sb.toString();
            var now = OffsetDateTime.now();
            var setUpToken = new PasswordSetUpToken(userId, hashed, now, now.plusDays(DAY_TILL_EXPIRE), null);
            
        } catch (NoSuchAlgorithmException e) {
            throw new InfrastructureException("SHA-512 algorithm not found: " + e.getMessage());
        }
    }

    
}
