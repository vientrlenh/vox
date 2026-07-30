package com.sep.vox.application.event;

public record RegisterFormRejectedPayloadV1(
    String to,
    String reason
) {
    
}
