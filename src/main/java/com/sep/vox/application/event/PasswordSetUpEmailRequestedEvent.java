package com.sep.vox.application.event;

import java.util.UUID;

public record PasswordSetUpEmailRequestedEvent(
    String to, 
    String schoolAdminName,
    String schoolName,
    UUID userId,
    String rawToken
) {
    
}
