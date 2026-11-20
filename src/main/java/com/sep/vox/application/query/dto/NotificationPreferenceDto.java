package com.sep.vox.application.query.dto;

import java.util.UUID;

public record NotificationPreferenceDto(
    UUID id, 
    UUID userId, 
    String category, 
    boolean pushEnabled, 
    boolean emailEnabled, 
    String updatedAt
) {
    
}
