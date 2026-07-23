package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;


import jakarta.validation.constraints.NotNull;

public record IssueMonitorTokenRequest(
    @NotNull(message = "Id của kỳ thi là bắt buộc")
    UUID examId,

    @NotNull(message = "Danh sách lịch thi là bắt buộc")
    List<UUID> scheduleIds
) {
}
