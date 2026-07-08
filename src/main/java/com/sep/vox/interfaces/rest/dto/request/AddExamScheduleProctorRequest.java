package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddExamScheduleProctorRequest(
    @NotNull(message = "Giáo viên là bắt buộc")
    UUID teacherId
) {
}
