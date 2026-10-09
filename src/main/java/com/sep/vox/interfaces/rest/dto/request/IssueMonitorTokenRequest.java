package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.IssueMonitorTokenCommand;

import jakarta.validation.constraints.NotNull;

public record IssueMonitorTokenRequest(
    @NotNull(message = "Id của kỳ thi là bắt buộc")
    UUID examId,

    @NotNull(message = "Danh sách lịch thi là bắt buộc")
    List<UUID> scheduleIds
) {

    public static IssueMonitorTokenCommand toCommand(IssueMonitorTokenRequest request) {
        return new IssueMonitorTokenCommand(request.examId, request.scheduleIds);
    }
}
