package com.sep.vox.infrastructure.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.sep.vox.infrastructure.properties.FirebaseProperties;

/**
 * Chỉ nạp khi {@code firebase.enabled=true}.
 *
 * <p>Cờ bật/tắt là bắt buộc chứ không phải tiện ích: thiếu nó thì mọi máy dev và mọi
 * @SpringBootTest chưa có service account đều chết ngay ở khâu load context, vì bean
 * này ném IOException khi không tìm thấy credentials. Khi tắt, NoOpPushNotificationService
 * đứng thay để phần còn lại của hệ thống vẫn chạy bình thường.
 */
@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseConfig.class);

    private static final Pattern URL_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]+:");

    @Bean
    FirebaseApp firebaseApp(FirebaseProperties properties, ResourceLoader resourceLoader) throws IOException {
        try (InputStream in = openCredentials(properties, resourceLoader)) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(in))
                .setConnectTimeout(5_000)
                .setReadTimeout(10_000)
                .build();

            // Whether getApps() is empty decides between initializing and reusing the old instance:
            // FirebaseApp keeps JVM-wide static state, so a devtools restart or a context
            // rebuilt in tests would throw IllegalStateException if initializeApp were called
            // directly a second time.
            if (FirebaseApp.getApps().isEmpty()) {
                LOGGER.info("Initializing FirebaseApp for push notifications");
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        }
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private InputStream openCredentials(FirebaseProperties properties, ResourceLoader resourceLoader)
            throws IOException {
        if (properties.hasInlineJson()) {
            return new ByteArrayInputStream(decodeInlineJson(properties.credentials().json()));
        }

        if (!properties.hasPath()) {
            throw new IllegalStateException(
                "firebase.enabled=true nhưng chưa cấu hình credentials: đặt firebase.credentials.path "
                    + "(đường dẫn file service account .json) hoặc firebase.credentials.json (nội dung JSON/base64)"
            );
        }

        String location = resolveLocation(properties.credentials().path());
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException(
                "Không tìm thấy Firebase service account tại: " + location
                    + " (giá trị cấu hình: " + properties.credentials().path() + ")"
            );
        }

        // A directory also satisfies exists() and can be opened as an InputStream, so if it is not blocked here
        // the error blows up later in GoogleCredentials.fromStream as a confusing JSON parse
        // error. Pointing at the folder that contains the file by mistake is very easy to do.
        if (resource.isFile() && resource.getFile().isDirectory()) {
            throw new IllegalStateException(
                "firebase.credentials.path đang trỏ vào một thư mục: " + location
                    + " -- cần trỏ thẳng tới file service account .json"
            );
        }

        return resource.getInputStream();
    }

    /**
     * A bare path like {@code C:\...} or {@code /etc/...} is understood by Spring as a
     * classpath resource and always throws FileNotFoundException, so the {@code file:}
     * scheme is prepended when the value has no scheme yet.
     *
     * <p>The scheme must be 2 or more characters long -- that is how {@code file:} or
     * {@code classpath:} is told apart from a Windows drive letter {@code C:}, which has only one character.
     */
    private String resolveLocation(String path) {
        String trimmed = path.trim();
        if (URL_SCHEME.matcher(trimmed).find()) {
            return trimmed;
        }
        // Spring resolves the URL with java.net.URL so the Windows \ cannot be used
        // in the path part; switch to / for consistency.
        return ResourceUtils.FILE_URL_PREFIX + trimmed.replace('\\', '/');
    }

    /** Accepts both raw JSON and base64 -- the environment variables of many platforms do not allow line breaks. */
    private byte[] decodeInlineJson(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("{")) {
            return trimmed.getBytes(StandardCharsets.UTF_8);
        }

        try {
            return Base64.getDecoder().decode(trimmed);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "firebase.credentials.json không phải JSON hợp lệ cũng không phải base64 hợp lệ", e
            );
        }
    }
}
