package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret, 
    Long expirationMs
) {
    public JwtProperties {
        if (secret == null || secret.strip().isBlank()) {
            throw new IllegalStateException("JWT secret is not found in configuration properties");
        }
        if (expirationMs == null || expirationMs == 0L) {
            expirationMs = 1800000L;
        }
    }
}
