package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.notification.NotificationDevicePlatform;

public record CreateNotificationDeviceCommand(
    String deviceId, 
    NotificationDevicePlatform platform, 
    String installationId
) {
    
}
