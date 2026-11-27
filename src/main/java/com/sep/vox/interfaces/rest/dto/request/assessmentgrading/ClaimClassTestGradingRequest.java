package com.sep.vox.interfaces.rest.dto.request.assessmentgrading;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.sep.vox.application.port.input.command.assessmentgrading.ClaimClassTestGradingCommand;
import com.sep.vox.domain.model.assessment.GradingRoundType;

public record ClaimClassTestGradingRequest(
    @NotNull(message = "Grading round is required")
    GradingRoundType roundType,

    @NotEmpty(message = "At least one result must be selected to claim for grading")
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
