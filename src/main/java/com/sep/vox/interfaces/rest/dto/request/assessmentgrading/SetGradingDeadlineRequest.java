package com.sep.vox.interfaces.rest.dto.request.assessmentgrading;

import com.sep.vox.application.port.input.command.assessmentgrading.SetGradingDeadlineCommand;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

/**
 * @param deadlineAt empty = remove the deadline from the selected assignments (the submission is no longer
 *        counted as overdue)
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
