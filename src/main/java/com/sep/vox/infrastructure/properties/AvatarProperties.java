package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.AvatarConfigPort;

/**
 * Avatar settings ({@code app.avatar.*}). A missing key gives an empty string, the same default the
 * former {@code @Value("${app.avatar.allowed-hosts:}")} produced.
 */
@ConfigurationProperties(prefix = "app.avatar")
public record AvatarProperties(
    String allowedHosts
) implements AvatarConfigPort {

    public AvatarProperties {
        allowedHosts = allowedHosts == null ? "" : allowedHosts;
    }
}
