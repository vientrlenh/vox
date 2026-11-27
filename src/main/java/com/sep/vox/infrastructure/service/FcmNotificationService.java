package com.sep.vox.infrastructure.service;

import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class FcmNotificationService implements PushNotificationPort {

    /**
     * Hard limit of sendEachForMulticast, verified in the firebase-admin 9.10.0
     * bytecode: "messages list must not contain more than 500 elements".
     */
    private static final int MAX_BATCH_SIZE = 500;

    private final FirebaseMessaging firebaseMessaging;

    /**
     * Prefix of the web-side redirect route. Empty is valid: the push is still sent, only the part
     * that opens the right entry when tapped from the browser tray is lost.
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

        List<String> targets = installationIds.stream()
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();

        if (targets.isEmpty()) {
            return PushDispatchResult.empty();
        }

        PushDispatchResult result = PushDispatchResult.empty();
        for (int start = 0; start < targets.size(); start += MAX_BATCH_SIZE) {
            List<String> batch = targets.subList(start, Math.min(start + MAX_BATCH_SIZE, targets.size()));
            result = result.merge(sendBatch(message, batch));
        }
        return result;
    }

    private PushDispatchResult sendBatch(PushMessage message, List<String> batch) {
        BatchResponse response;
        try {
            response = firebaseMessaging.sendEachForMulticast(buildMessage(message, batch));
        } catch (FirebaseMessagingException e) {
            // A batch-level failure (network lost, quota exhausted, wrong credentials) says nothing about
            // any single device, so the whole batch goes into the retryable group -- never delete.
            log.warn("FCM multicast failed for {} device(s), errorCode={}",
                batch.size(), e.getMessagingErrorCode(), e);
            return new PushDispatchResult(0, List.of(), batch);
        }

        List<String> stale = new ArrayList<String>();
        List<String> retryable = new ArrayList<String>();
        sortFailedInstallations(response.getResponses(), batch, stale, retryable);

        log.debug("FCM batch sent: success={}, stale={}, retryable={}",
            response.getSuccessCount(), stale.size(), retryable.size());

        return new PushDispatchResult(response.getSuccessCount(), stale, retryable);
    }

    private void sortFailedInstallations(
            List<SendResponse> responses, List<String> batch, List<String> stale, List<String> retryable) {
        // getResponses() keeps exactly the order of the FID list given to addAllFids, so the index
        // is the only bridge to tell which response belongs to which device.
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) {
                continue;
            }

            String installationId = batch.get(i);
            MessagingErrorCode errorCode = errorCodeOf(sendResponse.getException());
            if (isStale(errorCode)) {
                stale.add(installationId);
            } else {
                retryable.add(installationId);
            }

            if (errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
                log.error("FCM SENDER_ID_MISMATCH -- installation ID belongs to a different Firebase project, "
                    + "check the service account in use");
            }
        }
    }

    /**
     * Only these two error codes confirm that the device can never receive again
     * (UNREGISTERED: app removed or FID expired; INVALID_ARGUMENT: malformed FID).
     *
     * <p>Every other code -- UNAVAILABLE, INTERNAL, QUOTA_EXCEEDED, THIRD_PARTY_AUTH_ERROR,
     * SENDER_ID_MISMATCH, and also the null case -- must NOT be treated as dead. They reflect
     * an FCM-side incident or a wrong configuration, and will occur simultaneously on every device; deleting
     * because of them means wiping the table just because of one downtime.
     */
    private boolean isStale(MessagingErrorCode errorCode) {
        return errorCode == MessagingErrorCode.UNREGISTERED
            || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }

    private MessagingErrorCode errorCodeOf(FirebaseMessagingException exception) {
        return exception == null ? null : exception.getMessagingErrorCode();
    }

    private MulticastMessage buildMessage(PushMessage message, List<String> installationIds) {
        MulticastMessage.Builder builder = baseMessageBuilder(message, installationIds);

        String link = webpushLink(message);
        if (link != null) {
            builder.setWebpushConfig(WebpushConfig.builder()
                .setFcmOptions(WebpushFcmOptions.withLink(link))
                .build());
        }

        return builder.build();
    }

    private MulticastMessage.Builder baseMessageBuilder(PushMessage message, List<String> installationIds) {
        return MulticastMessage.builder()
            // addAllFids instead of addAllTokens: version 9.10.0 has @Deprecated the token branch.
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
    }

    /**
     * The path the browser opens when the user taps the notification tray while the tab is closed.
     *
     * <p>Only the web needs it: Android and iOS catch the tap themselves and read {@code data} inside the app, while the browser's
     * service worker has no route table to build a URL from. It points to
     * the redirect route {@code /n/{id}} instead of the real screen URL -- the route table is
     * the client's business and changes at any time, while {@code /n/{id}} does not.
     *
     * @return null when not configured, when the push lacks a notificationId (no way to know which entry to open),
     *         or when the configuration is not https -- FCM rejects a non-https link, and letting it
     *         through means losing the whole notification and not only the path.
     */
    // Not private: MulticastMessage has no getter, so the only way to check the URL
    // building is to call straight into here from a test in the same package.
    String webpushLink(PushMessage message) {
        if (notificationUrl.isBlank()) {
            return null;
        }

        String notificationId = message.data().get("notificationId");
        if (notificationId == null || notificationId.isBlank()) {
            return null;
        }

        if (!notificationUrl.startsWith("https://")) {
            log.warn("Skipping the webpush link because app.frontend.notification-url is not https: {}",
                notificationUrl);
            return null;
        }

        String prefix = notificationUrl.endsWith("/")
            ? notificationUrl.substring(0, notificationUrl.length() - 1)
            : notificationUrl;
        return prefix + "/" + notificationId;
    }
}
