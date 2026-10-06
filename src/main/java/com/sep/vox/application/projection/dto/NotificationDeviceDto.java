package com.sep.vox.application.projection.dto;

import java.util.UUID;

public record NotificationDeviceDto(
    UUID id, 
    UUID userId, 
    String deviceId, 
    String platform, 
    String installationId, 
    String createdAt, 
    String lastSeenAt
) {
    
}
