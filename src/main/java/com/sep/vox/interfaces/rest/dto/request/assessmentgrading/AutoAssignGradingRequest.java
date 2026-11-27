package com.sep.vox.interfaces.rest.dto.request.assessmentgrading;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.sep.vox.application.port.input.command.assessmentgrading.AutoAssignGradingCommand;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.GradingSampleSelectionMode;

/**
 * Either {@code assessmentId} or {@code scheduleId} must be present -- the "at least one" constraint lives in the
 * use case because Bean Validation cannot express it more concisely. The same goes for the constraint
 * "{@code percent} is required when the mode is RANDOM_PERCENT".
 *
 * @param selectionMode {@code ALL} | {@code RANDOM_PERCENT} | {@code RISK_BASED} |
 *                      {@code MANUAL_LIST}; empty = {@code ALL}
 */
public record AutoAssignGradingRequest(
    UUID assessmentId,

    UUID scheduleId,

    @NotNull(message = "A grading round must be selected")
    GradingRoundType roundType,

    GradingSampleSelectionMode selectionMode,

    @Min(value = 1, message = "Sampling rate must be at least 1%")
    @Max(value = 100, message = "Sampling rate must not exceed 100%")
    Integer percent,

    List<UUID> candidateResultIds,

    Instant deadlineAt,

    @NotEmpty(message = "At least one teacher must be selected for assignment")
    List<UUID> teacherIds
) {

    public static AutoAssignGradingCommand toCommand(AutoAssignGradingRequest request) {
        return new AutoAssignGradingCommand(
            request.assessmentId(),
            request.scheduleId(),
            request.roundType(),
            request.selectionMode(),
            request.percent(),
            request.candidateResultIds(),
            request.deadlineAt(),
            request.teacherIds()
        );
    }
}
