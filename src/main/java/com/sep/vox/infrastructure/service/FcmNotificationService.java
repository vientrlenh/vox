package com.sep.vox.infrastructure.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
public class FcmNotificationService {
    
    private final FirebaseMessaging firebaseMessaging;

    public FcmNotificationService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public void send(String installationId, String title, String body, Map<String, String> payload) {
        var notification = Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build();
        var message = Message.builder()
            .setFid(installationId)
            .setNotification(notification)
            .putAllData(payload == null ? Map.of() : payload)
            .setAndroidConfig(AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build())
            .setApnsConfig(ApnsConfig.builder()
                .setAps(Aps.builder().setSound("default").build())
                .build())
            .build();  
        
    }
}
