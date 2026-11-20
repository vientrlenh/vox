package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.notification.NotificationCategory;

public record NotificationPreferenceDto(
    UUID id, 
    UUID userId, 
    NotificationCategory category, 
    boolean pushEnabled, 
    boolean emailEnabled, 
    Instant updatedAt
) {
    
}
