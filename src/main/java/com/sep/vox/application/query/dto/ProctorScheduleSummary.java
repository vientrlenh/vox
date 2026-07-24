package com.sep.vox.application.query.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProctorScheduleSummary(
    UUID scheduleId,
    UUID examId,
    String examName,
    UUID schoolRoomId,
    String roomName,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    String status
) {
}
