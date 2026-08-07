package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewMyNotificationsCursorPageQuery(
    UUID cursor,
    int limit
) {
}
