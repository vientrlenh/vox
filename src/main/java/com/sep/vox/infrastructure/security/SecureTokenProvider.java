package com.sep.vox.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sep.vox.infrastructure.exception.InfrastructureException;

@Component
public class SecureTokenProvider {
    
    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String DIGITS = "0123456789";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String SHA_512_HASH_METHOD = "SHA-512";
    private static final String SHA_256_HASH_METHOD = "SHA-256";

    public String generateToken(int length) {
        return SECURE_RANDOM.ints(length, 0, ALPHA_NUMERIC.length())
            .mapToObj(ALPHA_NUMERIC::charAt)
            .map(Object::toString)
            .collect(Collectors.joining());
    }

    public String generateDigits(int length) {
        return SECURE_RANDOM.ints(length, 0, DIGITS.length())
            .mapToObj(DIGITS::charAt)
            .map(Object::toString)
            .collect(Collectors.joining());
    }

    public String sha512(String rawToken) {
        return hash(rawToken, SHA_512_HASH_METHOD);
    }

    public String sha256(String rawToken) {
        return hash(rawToken, SHA_256_HASH_METHOD);
    }

    private String hash(String raw, String method) {
        try {
            var md = MessageDigest.getInstance(method);
            var hashBytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));

            var sb = new StringBuilder();
            for (byte b : hashBytes) {
                var hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    sb.append('0');
                } 
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new InfrastructureException(method + " algorithm not found: " + e.getMessage());
        } 
    }
}
