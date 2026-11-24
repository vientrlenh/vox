package com.sep.vox.interfaces.rest.dto.request.assessmentgrading;

import com.sep.vox.application.port.input.command.assessmentgrading.GradingDecisionCommand;
import java.util.UUID;
import jakarta.validation.constraints.Size;

/**
 * Shared body of the actions that do not enter a score.
 *
 * <p>{@code reason} is not marked {@code @NotBlank} here because whether it is required depends on
 * the action (optional for {@code UPHOLD}, required for the other three) --
 * that rule lives in {@code GradingRoundPolicy} and is not duplicated down into the DTO.
 */
public record GradingDecisionRequest(
    @Size(max = 1024, message = "Lý do tối đa 1024 ký tự")
    String reason
) {

    /** Shared by /uphold, /invalidate, /clear-invalid and /decline: they take the same body. */
    public static GradingDecisionCommand toCommand(UUID assignmentId, GradingDecisionRequest request) {
        return new GradingDecisionCommand(assignmentId, request == null ? null : request.reason());
    }
}
