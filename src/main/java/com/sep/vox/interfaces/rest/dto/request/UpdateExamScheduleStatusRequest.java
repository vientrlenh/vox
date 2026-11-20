package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record UpdateExamScheduleStatusRequest(
    @NotBlank(message = "Hành động là bắt buộc")
    String action,

    String note,

    UUID targetScheduleId
) {
}
