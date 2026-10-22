package com.sep.vox.application.event;

public record RegisterFormRejectedPayload(
    String to,
    String reason
) {
    
}
