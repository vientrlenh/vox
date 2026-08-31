package com.sep.vox.infrastructure.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.sep.vox.application.port.output.PushNotificationPort;
import com.sep.vox.application.response.output.PushDispatchResult;
import com.sep.vox.application.response.output.PushMessage;

@Service
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class FcmNotificationService implements PushNotificationPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(FcmNotificationService.class);

    /**
     * Giới hạn cứng của sendEachForMulticast, kiểm chứng trong bytecode firebase-admin
     * 9.10.0: "messages list must not contain more than 500 elements".
     */
    private static final int MAX_BATCH_SIZE = 500;

    private final FirebaseMessaging firebaseMessaging;

    /**
     * Tiền tố của route chuyển hướng phía web. Rỗng là hợp lệ: push vẫn gửi, chỉ mất phần
     * mở đúng mục khi bấm từ khay của trình duyệt.
     */
    private final String notificationUrl;

    public FcmNotificationService(
            FirebaseMessaging firebaseMessaging,
            @Value("${app.frontend.notification-url:}") String notificationUrl) {
        this.firebaseMessaging = firebaseMessaging;
        this.notificationUrl = notificationUrl == null ? "" : notificationUrl.trim();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public PushDispatchResult send(PushMessage message, List<String> installationIds) {
        if (installationIds == null || installationIds.isEmpty()) {
            return PushDispatchResult.empty();
        }

        var targets = installationIds.stream()
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();

        if (targets.isEmpty()) {
            return PushDispatchResult.empty();
        }

        var result = PushDispatchResult.empty();
        for (int start = 0; start < targets.size(); start += MAX_BATCH_SIZE) {
            var batch = targets.subList(start, Math.min(start + MAX_BATCH_SIZE, targets.size()));
            result = result.merge(sendBatch(message, batch));
        }
        return result;
    }

    private PushDispatchResult sendBatch(PushMessage message, List<String> batch) {
        BatchResponse response;
        try {
            response = firebaseMessaging.sendEachForMulticast(buildMessage(message, batch));
        } catch (FirebaseMessagingException e) {
            // Lỗi ở mức cả lô (mất mạng, hết quota, credentials sai) không nói gì về
            // từng thiết bị, nên toàn bộ lô vào nhóm retryable -- tuyệt đối không xoá.
            LOGGER.warn("FCM multicast failed for {} device(s), errorCode={}",
                batch.size(), e.getMessagingErrorCode(), e);
            return new PushDispatchResult(0, List.of(), batch);
        }

        var stale = new ArrayList<String>();
        var retryable = new ArrayList<String>();

        // getResponses() giữ đúng thứ tự của danh sách FID đã addAllFids, nên chỉ số
        // là cầu nối duy nhất để biết response nào ứng với thiết bị nào.
        var responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            var sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) {
                continue;
            }

            var installationId = batch.get(i);
            var errorCode = errorCodeOf(sendResponse.getException());
            if (isStale(errorCode)) {
                stale.add(installationId);
            } else {
                retryable.add(installationId);
            }

            if (errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
                LOGGER.error("FCM SENDER_ID_MISMATCH -- installation ID thuộc Firebase project khác, "
                    + "kiểm tra lại service account đang dùng");
            }
        }

        LOGGER.debug("FCM batch sent: success={}, stale={}, retryable={}",
            response.getSuccessCount(), stale.size(), retryable.size());

        return new PushDispatchResult(response.getSuccessCount(), stale, retryable);
    }

    /**
     * Chỉ hai mã lỗi này khẳng định thiết bị vĩnh viễn không nhận được nữa
     * (UNREGISTERED: đã gỡ app hoặc FID hết hạn; INVALID_ARGUMENT: FID sai định dạng).
     *
     * <p>Mọi mã còn lại -- UNAVAILABLE, INTERNAL, QUOTA_EXCEEDED, THIRD_PARTY_AUTH_ERROR,
     * SENDER_ID_MISMATCH, và cả trường hợp null -- đều KHÔNG được coi là chết. Chúng phản
     * ánh sự cố phía FCM hoặc cấu hình sai, sẽ xảy ra đồng loạt trên mọi thiết bị; xoá
     * theo chúng đồng nghĩa với xoá sạch bảng chỉ vì một lần downtime.
     */
    private boolean isStale(MessagingErrorCode errorCode) {
        return errorCode == MessagingErrorCode.UNREGISTERED
            || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }

    private MessagingErrorCode errorCodeOf(FirebaseMessagingException exception) {
        return exception == null ? null : exception.getMessagingErrorCode();
    }

    private MulticastMessage buildMessage(PushMessage message, List<String> installationIds) {
        var builder = MulticastMessage.builder()
            // addAllFids thay cho addAllTokens: bản 9.10.0 đã @Deprecated nhánh token.
            .addAllFids(installationIds)
            .setNotification(Notification.builder()
                .setTitle(message.title())
                .setBody(message.body())
                .build())
            .putAllData(message.data())
            .setAndroidConfig(AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build())
            .setApnsConfig(ApnsConfig.builder()
                .setAps(Aps.builder()
                    .setSound("default")
                    .build())
                .build());

        var link = webpushLink(message);
        if (link != null) {
            builder.setWebpushConfig(WebpushConfig.builder()
                .setFcmOptions(WebpushFcmOptions.withLink(link))
                .build());
        }

        return builder.build();
    }

    /**
     * Đường dẫn trình duyệt mở khi người dùng bấm vào khay thông báo lúc tab đã đóng.
     *
     * <p>Chỉ web mới cần: Android và iOS tự bắt cú bấm rồi đọc {@code data} trong app, còn
     * service worker của trình duyệt thì không có sẵn bảng route nào để tự dựng URL. Trỏ
     * vào route chuyển hướng {@code /n/{id}} thay vì URL màn hình thật -- bảng route là
     * chuyện của client và đổi bất cứ lúc nào, còn {@code /n/{id}} thì không.
     *
     * @return null khi chưa cấu hình, khi push thiếu notificationId (không biết mở mục nào),
     *         hoặc khi cấu hình không phải https -- FCM từ chối link không https, và để nó
     *         đi tiếp nghĩa là mất luôn cả thông báo chứ không chỉ mất đường dẫn.
     */
    // Không private: MulticastMessage không có getter nào, nên cách duy nhất để kiểm phần
    // dựng URL là gọi thẳng vào đây từ test cùng package.
    String webpushLink(PushMessage message) {
        if (notificationUrl.isBlank()) {
            return null;
        }

        var notificationId = message.data().get("notificationId");
        if (notificationId == null || notificationId.isBlank()) {
            return null;
        }

        if (!notificationUrl.startsWith("https://")) {
            LOGGER.warn("Bỏ qua link webpush vì app.frontend.notification-url không phải https: {}",
                notificationUrl);
            return null;
        }

        var prefix = notificationUrl.endsWith("/")
            ? notificationUrl.substring(0, notificationUrl.length() - 1)
            : notificationUrl;
        return prefix + "/" + notificationId;
    }
}
