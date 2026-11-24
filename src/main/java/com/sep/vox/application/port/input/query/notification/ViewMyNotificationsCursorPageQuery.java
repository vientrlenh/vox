package com.sep.vox.application.port.input.query.notification;

import java.util.UUID;

public record ViewMyNotificationsCursorPageQuery(
    UUID cursor,
    int limit
) {
}
