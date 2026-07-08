package com.sep.vox.interfaces.rest.dto.request;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateExamScheduleRequest(
    @NotNull(message = "Phòng học là bắt buộc")
    UUID schoolRoomId,

    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    OffsetDateTime startDate,

    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    OffsetDateTime endDate
) {
}
