package com.sep.vox.infrastructure.shared;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public class TokenHasher {
    
    private static final String SHA_256 = "SHA-256";
    private static final String SHA_512 = "SHA-512";

    public static String sha256(String raw) {
        return hash(raw, SHA_256);
    }

    public static String sha512(String raw) {
        return hash(raw, SHA_512);
    }

    private static String hash(String raw, String method) {
        try {
            MessageDigest md = MessageDigest.getInstance(method);
            byte[] hashBytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff&b);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            log.error("No hashed algorithm for {}: {}", method, ex.getMessage(), ex);
            throw new IllegalStateException("Internal server error");
        }
    }
}
