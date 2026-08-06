package com.sep.vox.infrastructure.service;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

@Service
public class FcmNotificationService {
    
    private final FirebaseMessaging firebaseMessaging;

    public FcmNotificationService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }
}
