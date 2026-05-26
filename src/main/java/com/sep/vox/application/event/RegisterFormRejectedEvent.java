package com.sep.vox.application.event;

public record RegisterFormRejectedEvent(
    String to,
    String reason
) {
    
}
