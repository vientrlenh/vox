package com.sep.vox.interfaces.rest.dto.request;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateAssessmentScheduleRequest(
    @NotNull(message = "Phòng học là bắt buộc")
    UUID schoolRoomId,

    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    Instant startDate,

    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    Instant endDate
) {
}
