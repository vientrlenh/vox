package com.sep.vox.application.port.input.command.notification;

import java.util.UUID;

public record MarkNotificationAsReadCommand(
    UUID id
) {
    
}
