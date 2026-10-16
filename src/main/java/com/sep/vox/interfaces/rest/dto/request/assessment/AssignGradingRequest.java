package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AssignGradingCommand;
import com.sep.vox.application.port.input.command.AssignGradingCommand.AssignmentItemCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


public record AssignGradingRequest(
    @NotBlank(message = "Phải chọn vòng chấm")
    String roundType,

    Instant deadlineAt,

    @NotEmpty(message = "Phải chọn ít nhất một bài thi để phân công")
    @Valid
    List<AssignmentItemRequest> assignments
) {
    public record AssignmentItemRequest(
        @NotNull(message = "Thiếu bài thi cần phân công")
        UUID candidateResultId,

        @NotNull(message = "Thiếu giáo viên cần phân công")
        UUID teacherId
    ) {
    }

    public static AssignGradingCommand toCommand(AssignGradingRequest request) {
        return new AssignGradingCommand(
            request.roundType, 
            request.deadlineAt, 
            request.assignments.stream()
                .map(a -> new AssignmentItemCommand(
                    a.candidateResultId, 
                    a.teacherId
                )).toList()
        );
    }
}
