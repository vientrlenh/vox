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
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String SHA_512_HASH_METHOD = "SHA-512";

    public String generateToken(int length) {
        return SECURE_RANDOM.ints(length, 0, ALPHA_NUMERIC.length())
            .mapToObj(ALPHA_NUMERIC::charAt)
            .map(Object::toString)
            .collect(Collectors.joining());
    }

    public String sha512(String rawToken) {
        try {
            var md = MessageDigest.getInstance(SHA_512_HASH_METHOD);
            var hashBytes = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));

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
            throw new InfrastructureException("SHA-512 algorithm not found: " + e.getMessage());
        } 
    }
}
