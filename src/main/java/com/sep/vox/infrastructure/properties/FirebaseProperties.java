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
     * Hai cách nạp service account, dùng đúng một trong hai.
     *
     * @param path đường dẫn tới file .json. Chấp nhận cả dạng có scheme
     *             ({@code file:}, {@code classpath:}) lẫn đường dẫn tuyệt đối trần --
     *             xem FirebaseConfig#resolveLocation.
     * @param json nội dung service account truyền thẳng, dạng JSON thô hoặc base64.
     *             Dành cho môi trường container không mount được file.
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
