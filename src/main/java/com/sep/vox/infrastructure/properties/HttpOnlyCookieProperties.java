package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cookie")
public record HttpOnlyCookieProperties(
    Boolean secure, 
    String sameSite
) {
    public HttpOnlyCookieProperties {
        if (secure == null) {
            secure = Boolean.FALSE;
        }
        if (sameSite == null || sameSite.strip().isBlank()) {
            sameSite = "None";
        }
    }
}
