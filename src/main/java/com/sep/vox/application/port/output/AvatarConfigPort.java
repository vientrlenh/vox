package com.sep.vox.application.port.output;

/**
 * Avatar settings read from configuration ({@code app.avatar.*}), bound by AvatarProperties.
 */
public interface AvatarConfigPort {

    /**
     * Comma-separated hosts allowed in {@code users.avatar_url}; empty when nothing is configured.
     * The string is handed to AvatarUrlPolicy unchanged.
     */
    String allowedHosts();
}
