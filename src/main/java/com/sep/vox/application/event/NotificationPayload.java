package com.sep.vox.application.event;

import java.util.UUID;

public record NotificationPayload(
    UUID userId, 
    String title, 
    String body, 
    String payload
) {
    
}
