package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.notification.NotificationDevicePlatform;

public record NotificationDeviceDto(
    UUID id, 
    UUID userId, 
    String deviceId, 
    NotificationDevicePlatform platform, 
    String installationId, 
    Instant createdAt, 
    Instant lastSeenAt
) {
    
}
