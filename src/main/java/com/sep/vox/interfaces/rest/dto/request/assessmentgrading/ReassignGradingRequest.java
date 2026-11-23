package com.sep.vox.interfaces.rest.dto.request.assessmentgrading;

import com.sep.vox.application.port.input.command.assessmentgrading.ReassignGradingCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ReassignGradingRequest(
    @NotNull(message = "Thiếu giáo viên cần phân công")
    UUID teacherId
) {

    public static ReassignGradingCommand toCommand(UUID assignmentId, ReassignGradingRequest request) {
        return new ReassignGradingCommand(assignmentId, request.teacherId());
    }
}
