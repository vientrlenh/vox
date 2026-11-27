package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(
    boolean enabled,
    Credentials credentials
) {
    public FirebaseProperties {
        credentials = credentials == null ? new Credentials(null, null) : credentials;
    }

    /**
     * Two ways to load the service account, use exactly one of them.
     *
     * @param path path to the .json file. Accepts both a form with a scheme
     *             ({@code file:}, {@code classpath:}) and a bare absolute path --
     *             see FirebaseConfig#resolveLocation.
     * @param json service account content passed directly, as raw JSON or base64.
     *             For container environments that cannot mount a file.
     */
    public record Credentials(String path, String json) {
    }

    public boolean hasInlineJson() {
        return credentials.json() != null && !credentials.json().isBlank();
    }

    public boolean hasPath() {
        return credentials.path() != null && !credentials.path().isBlank();
    }
}
