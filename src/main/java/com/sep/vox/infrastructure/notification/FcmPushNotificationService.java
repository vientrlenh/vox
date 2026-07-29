package com.sep.vox.infrastructure.notification;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.sep.vox.application.port.output.PushNotificationPort;
import com.sep.vox.domain.repository.DeviceSessionRepository;

import jakarta.annotation.PostConstruct;

@Service
public class FcmPushNotificationService implements PushNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationService.class);

    private final DeviceSessionRepository deviceSessionRepository;
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${app.firebase.credentials-path}")
    private String credentialsPath;

    private FirebaseApp firebaseApp;

    public FcmPushNotificationService(DeviceSessionRepository deviceSessionRepository) {
        this.deviceSessionRepository = deviceSessionRepository;
    }

    @PostConstruct
    void init() {
        try (InputStream credentialsStream = resourceLoader.getResource(credentialsPath).getInputStream()) {
            var options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                .build();
            this.firebaseApp = FirebaseApp.getApps().isEmpty()
                ? FirebaseApp.initializeApp(options)
                : FirebaseApp.getInstance();
        } catch (IOException | RuntimeException e) {
            log.warn("Firebase credentials not available at {}, push notifications disabled: {}",
                credentialsPath, e.getMessage());
            this.firebaseApp = null;
        }
    }

    @Override
    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        sendToUsers(List.of(userId), title, body, data);
    }

    @Override
    public void sendToUsers(List<UUID> userIds, String title, String body, Map<String, String> data) {
        if (firebaseApp == null) {
            log.debug("Firebase not initialized, skipping push notification to {} user(s)", userIds.size());
            return;
        }

        var tokens = new ArrayList<String>();
        for (var userId : userIds) {
            deviceSessionRepository.findActivePushTokensByUserId(userId).stream()
                .map(session -> session.getPushToken())
                .forEach(tokens::add);
        }
        if (tokens.isEmpty()) {
            log.debug("No active push tokens found for {} user(s), skipping", userIds.size());
            return;
        }

        var notification = Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build();
        var messages = tokens.stream()
            .map(token -> toMessage(token, notification, data))
            .toList();

        try {
            var response = FirebaseMessaging.getInstance(firebaseApp).sendEach(messages);
            if (response.getFailureCount() > 0) {
                response.getResponses().forEach(sendResponse -> {
                    if (!sendResponse.isSuccessful()) {
                        var exception = sendResponse.getException();
                        MessagingErrorCode code = exception == null ? null : exception.getMessagingErrorCode();
                        log.warn("Push notification delivery failed: {}", code);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to send push notification: {}", e.getMessage());
        }
    }

    private static Message toMessage(String token, Notification notification, Map<String, String> data) {
        return Message.builder()
            .setToken(token)
            .setNotification(notification)
            .putAllData(data == null ? Map.of() : data)
            .build();
    }
}
