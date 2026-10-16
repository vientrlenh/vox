package com.sep.vox.application.projection.dto;

import java.time.Instant;
import java.util.UUID;

public record ProctorScheduleSummary(
    UUID scheduleId,
    UUID assessmentId,
    String assessmentName,
    UUID schoolRoomId,
    String roomName,
    Instant startDate,
    Instant endDate,
    String status
) {
}
