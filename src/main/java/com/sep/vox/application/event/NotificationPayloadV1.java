package com.sep.vox.application.event;

import java.util.UUID;

public record NotificationPayloadV1(
    UUID userId, 
    String title, 
    String body, 
    String payload
) {
    
}
