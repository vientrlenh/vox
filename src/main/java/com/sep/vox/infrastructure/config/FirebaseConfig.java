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
        try (var in = openCredentials(properties, resourceLoader)) {
            var options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(in))
                .setConnectTimeout(5_000)
                .setReadTimeout(10_000)
                .build();

            // getApps() rỗng hay không quyết định initialize hay lấy lại instance cũ:
            // FirebaseApp giữ state tĩnh toàn JVM, nên devtools restart hoặc context
            // được dựng lại trong test sẽ ném IllegalStateException nếu gọi thẳng
            // initializeApp lần hai.
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

        var location = resolveLocation(properties.credentials().path());
        var resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException(
                "Không tìm thấy Firebase service account tại: " + location
                    + " (giá trị cấu hình: " + properties.credentials().path() + ")"
            );
        }

        // Thư mục cũng thoả exists() và mở được InputStream, nên nếu không chặn ở đây
        // thì lỗi sẽ nổ muộn hơn trong GoogleCredentials.fromStream dưới dạng lỗi parse
        // JSON khó hiểu. Trỏ nhầm vào thư mục chứa file là nhầm lẫn rất dễ mắc.
        if (resource.isFile() && resource.getFile().isDirectory()) {
            throw new IllegalStateException(
                "firebase.credentials.path đang trỏ vào một thư mục: " + location
                    + " -- cần trỏ thẳng tới file service account .json"
            );
        }

        return resource.getInputStream();
    }

    /**
     * Đường dẫn trần kiểu {@code C:\...} hay {@code /etc/...} bị Spring hiểu là
     * classpath resource và luôn ném FileNotFoundException, nên thêm sẵn scheme
     * {@code file:} khi giá trị chưa có scheme nào.
     *
     * <p>Scheme phải dài từ 2 ký tự trở lên -- đó là cách phân biệt {@code file:} hay
     * {@code classpath:} với ký tự ổ đĩa Windows {@code C:}, vốn chỉ có một ký tự.
     */
    private String resolveLocation(String path) {
        var trimmed = path.trim();
        if (URL_SCHEME.matcher(trimmed).find()) {
            return trimmed;
        }
        // Spring resolve URL bằng java.net.URL nên dấu \ của Windows không dùng được
        // trong phần path; đổi sang / cho đồng nhất.
        return ResourceUtils.FILE_URL_PREFIX + trimmed.replace('\\', '/');
    }

    /** Nhận cả JSON thô lẫn base64 -- biến môi trường nhiều nền tảng không cho xuống dòng. */
    private byte[] decodeInlineJson(String value) {
        var trimmed = value.trim();
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
