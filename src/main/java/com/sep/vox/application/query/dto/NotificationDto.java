package com.sep.vox.application.query.dto;

import java.util.UUID;

public record NotificationDto(
    UUID id, 
    UUID userId, 
    UUID eventId, 
    String eventType, 
    String title, 
    String body, 
    String payload, 
    String readAt, 
    String createdAt
) {
    
}
