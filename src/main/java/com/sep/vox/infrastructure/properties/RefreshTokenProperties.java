package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(
    String seed, 
    Long expirationMs, 
    Integer length
) {
    public RefreshTokenProperties {
        if (seed == null || seed.strip().isBlank()) {
            throw new IllegalStateException("Refresh token seed is not configured in configuration properties");
        }
        if (expirationMs == null || expirationMs <= 0L) {
            expirationMs = 60480000L;
        }
        if (length == null || length <= 0) {
            length = 64;
        }
    }
}
