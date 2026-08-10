package com.sep.vox.application.event;

import java.time.Instant;
import java.util.UUID;

public record DeviceSessionRevokedEvent(
    UUID sessionId,
    UUID userId,
    String deviceId,
    Instant revokedAt
) {
}
