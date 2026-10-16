package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.ClaimClassTestGradingCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ClaimClassTestGradingRequest(
    @NotBlank(message = "Vòng chấm là bắt buộc")
    String roundType,

    @NotEmpty(message = "Phải chọn ít nhất một bài để nhận chấm")
    List<UUID> candidateResultIds
) {

    public static ClaimClassTestGradingCommand toCommand(UUID assessmentId, ClaimClassTestGradingRequest request) {
        return new ClaimClassTestGradingCommand(
            assessmentId,
            request.roundType(),
            request.candidateResultIds()
        );
    }
}
