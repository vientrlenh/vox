package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ReassignGradingRequest(
    @NotNull(message = "Thiếu giáo viên cần phân công")
    UUID teacherId
) {
}
