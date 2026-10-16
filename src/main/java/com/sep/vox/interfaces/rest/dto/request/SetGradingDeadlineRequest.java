package com.sep.vox.interfaces.rest.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.SetGradingDeadlineCommand;

import jakarta.validation.constraints.NotEmpty;

/**
 * @param deadlineAt bỏ trống = gỡ hạn cho các phân công được chọn (bài không còn bị
 *        tính là quá hạn)
 */
public record SetGradingDeadlineRequest(
    @NotEmpty(message = "Phải chọn ít nhất một phân công để đặt hạn")
    List<UUID> assignmentIds,

    Instant deadlineAt
) {

    public static SetGradingDeadlineCommand toCommand(SetGradingDeadlineRequest request) {
        return new SetGradingDeadlineCommand(request.assignmentIds(), request.deadlineAt());
    }
}
