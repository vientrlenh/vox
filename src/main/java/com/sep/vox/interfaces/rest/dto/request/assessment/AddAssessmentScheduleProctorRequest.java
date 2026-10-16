package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddAssessmentScheduleProctorRequest(
    @NotNull(message = "Giáo viên là bắt buộc")
    UUID teacherId
) {
}
