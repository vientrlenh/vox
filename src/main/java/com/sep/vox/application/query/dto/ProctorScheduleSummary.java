package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

public record ProctorScheduleSummary(
    UUID scheduleId,
    UUID examId,
    String examName,
    UUID schoolRoomId,
    String roomName,
    Instant startDate,
    Instant endDate,
    String status
) {
}
