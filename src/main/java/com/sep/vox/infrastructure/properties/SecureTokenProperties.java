package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.secure-token")
public record SecureTokenProperties(
    String seed, 
    Integer length
) {
    public SecureTokenProperties {
        if (seed == null || seed.strip().isBlank()) {
            throw new IllegalStateException("Secure token seed is not configured in configuration properties");
        }
        if (length == null || length <= 0) {
            length = 32;
        }
    }
}
