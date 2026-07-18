package com.sep.vox.application.response.input.exam;

import java.util.UUID;

public record ProctorScheduleSummaryResponse(
    UUID scheduleId,
    UUID examId,
    String examName,
    UUID schoolRoomId,
    String roomName,
    String startDate,
    String endDate,
    String status
) {
}
