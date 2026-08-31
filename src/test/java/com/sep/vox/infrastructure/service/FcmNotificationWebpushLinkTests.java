package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.firebase.messaging.FirebaseMessaging;
import com.sep.vox.application.response.output.PushMessage;

/**
 * Đường dẫn gắn vào {@code webpush.fcmOptions.link} -- thứ trình duyệt mở khi người dùng
 * bấm vào khay thông báo lúc tab đã đóng.
 *
 * <p>Chỉ kiểm phần dựng URL, không kiểm phần gọi FCM: {@code MulticastMessage} không phơi
 * getter nào nên không assert được vào message đã dựng, còn bản thân lời gọi thư viện thì
 * đã có {@code PushNotificationWiringTests} lo phần lắp ráp.
 */
class FcmNotificationWebpushLinkTests {

    private static final String NOTIFICATION_ID = "6d1f1c9e-6d8e-4f2a-9d1a-2f7c5b0a1e33";

    private FcmNotificationService service(String notificationUrl) {
        return new FcmNotificationService(mock(FirebaseMessaging.class), notificationUrl);
    }

    private PushMessage message(Map<String, String> data) {
        return new PushMessage("Điểm thi của bạn đã có", "Kỳ thi giữa kỳ", data);
    }

    private PushMessage messageWithId() {
        return message(Map.of("notificationId", NOTIFICATION_ID, "target", "EXAM_RESULT_DETAIL"));
    }

    @Test
    void should_point_at_the_redirect_route_of_the_notification() {
        var link = service("https://app.vox.edu.vn/n").webpushLink(messageWithId());

        assertThat(link).isEqualTo("https://app.vox.edu.vn/n/" + NOTIFICATION_ID);
    }

    /** Cấu hình có hay không có dấu / ở cuối đều phải ra cùng một URL, không ra "//". */
    @Test
    void should_not_double_the_slash_when_configured_url_ends_with_one() {
        var link = service("https://app.vox.edu.vn/n/").webpushLink(messageWithId());

        assertThat(link).isEqualTo("https://app.vox.edu.vn/n/" + NOTIFICATION_ID);
    }

    /** Chưa cấu hình: push vẫn phải gửi được, chỉ là bấm vào không mở đúng mục. */
    @Test
    void should_skip_the_link_when_not_configured() {
        assertThat(service("").webpushLink(messageWithId())).isNull();
        assertThat(service("   ").webpushLink(messageWithId())).isNull();
        assertThat(service(null).webpushLink(messageWithId())).isNull();
    }

    /**
     * FCM từ chối link không phải https. Để nó đi tiếp nghĩa là mất cả thông báo chứ không
     * chỉ mất đường dẫn -- đúng cái bẫy mà cấu hình http://localhost lúc dev sẽ giăng ra.
     */
    @Test
    void should_skip_the_link_when_configured_url_is_not_https() {
        assertThat(service("http://localhost:5173/n").webpushLink(messageWithId())).isNull();
    }

    /** Không có id thì không biết mở mục nào; gửi link cụt còn tệ hơn không gửi link. */
    @Test
    void should_skip_the_link_when_the_push_carries_no_notification_id() {
        assertThat(service("https://app.vox.edu.vn/n")
            .webpushLink(message(Map.of("target", "EXAM_RESULT_DETAIL")))).isNull();
    }
}
