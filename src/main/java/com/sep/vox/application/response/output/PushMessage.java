package com.sep.vox.application.response.output;

import java.util.Map;

/**
 * The content of a push notification, independent of the provider.
 *
 * <p>{@code data} is the payload the client uses to navigate when the user taps the notification
 * (for example {@code screen}, {@code assessmentId}). FCM only accepts Map&lt;String, String&gt;,
 * so every value must be stringified before it gets here.
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
