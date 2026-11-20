package com.sep.vox.application.response.output;

import java.util.Map;

/**
 * Nội dung một thông báo đẩy, không phụ thuộc nhà cung cấp.
 *
 * <p>{@code data} là payload để client điều hướng khi người dùng bấm vào thông báo
 * (ví dụ {@code screen}, {@code examId}). FCM chỉ nhận Map&lt;String, String&gt;,
 * nên mọi giá trị phải được stringify từ trước khi tới đây.
 */
public record PushMessage(
    String title,
    String body,
    Map<String, String> data
) {
    public PushMessage {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("push message title must not be null or blank");
        }
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public PushMessage(String title, String body) {
        this(title, body, Map.of());
    }
}
