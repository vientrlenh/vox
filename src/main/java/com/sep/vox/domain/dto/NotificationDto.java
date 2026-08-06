package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
    UUID id, 
    UUID userId, 
    UUID eventId, 
    String eventType, 
    String title, 
    String body, 
    String payload, 
    Instant readAt, 
    Instant createdAt
) {
    
}
