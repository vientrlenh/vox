package com.sep.vox.interfaces.kafka.dto;

public record UserRegisteredPayload(
    String userId,
    String email,
    String fullName
) {
}
