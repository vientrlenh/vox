package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.firebase.messaging.FirebaseMessaging;
import com.sep.vox.application.response.output.PushMessage;

/**
 * The path attached to {@code webpush.fcmOptions.link} -- what the browser opens when the user
 * taps the notification tray while the tab is closed.
 *
 * <p>Only checks the URL building, not the FCM call: {@code MulticastMessage} exposes no
 * getter so the built message cannot be asserted on, and the library call itself is already
 * covered for assembly by {@code PushNotificationWiringTests}.
 */
class FcmNotificationWebpushLinkTests {

    private static final String NOTIFICATION_ID = "6d1f1c9e-6d8e-4f2a-9d1a-2f7c5b0a1e33";

    private FcmNotificationService service(String notificationUrl) {
        return new FcmNotificationService(mock(FirebaseMessaging.class), notificationUrl);
    }

    private PushMessage message(Map<String, String> data) {
        return new PushMessage("Your assessment score is available", "Kỳ thi giữa kỳ", data);
    }

    private PushMessage messageWithId() {
        return message(Map.of("notificationId", NOTIFICATION_ID, "target", "ASSESSMENT_RESULT_DETAIL"));
    }

    @Test
    void should_point_at_the_redirect_route_of_the_notification() {
        String link = service("https://app.vox.edu.vn/n").webpushLink(messageWithId());

        assertThat(link).isEqualTo("https://app.vox.edu.vn/n/" + NOTIFICATION_ID);
    }

    /** A configuration with or without a trailing / must produce the same URL, not "//". */
    @Test
    void should_not_double_the_slash_when_configured_url_ends_with_one() {
        String link = service("https://app.vox.edu.vn/n/").webpushLink(messageWithId());

        assertThat(link).isEqualTo("https://app.vox.edu.vn/n/" + NOTIFICATION_ID);
    }

    /** Not configured: the push must still be sendable, tapping it just does not open the right entry. */
    @Test
    void should_skip_the_link_when_not_configured() {
        assertThat(service("").webpushLink(messageWithId())).isNull();
        assertThat(service("   ").webpushLink(messageWithId())).isNull();
        assertThat(service(null).webpushLink(messageWithId())).isNull();
    }

    /**
     * FCM rejects a link that is not https. Letting it through means losing the whole notification and not
     * only the path -- exactly the trap that an http://localhost configuration in dev would set.
     */
    @Test
    void should_skip_the_link_when_configured_url_is_not_https() {
        assertThat(service("http://localhost:5173/n").webpushLink(messageWithId())).isNull();
    }

    /** Without an id there is no way to know which entry to open; sending a truncated link is worse than sending no link. */
    @Test
    void should_skip_the_link_when_the_push_carries_no_notification_id() {
        assertThat(service("https://app.vox.edu.vn/n")
            .webpushLink(message(Map.of("target", "ASSESSMENT_RESULT_DETAIL")))).isNull();
    }
}
