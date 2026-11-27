package com.sep.vox.interfaces.rest.dto.request.assessmentgrading;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.sep.vox.application.port.input.command.assessmentgrading.AssignGradingCommand;
import com.sep.vox.domain.model.assessment.GradingRoundType;

/**
 * Manually assigns several submissions at once -- the admin ticks several rows and assigns them in one go.
 *
 * <p>One assignment has a single {@code roundType}: in the admin UI the round is also chosen first and
 * the submissions are ticked afterwards, and mixing rounds in one batch makes the status check rules ambiguous.
 */
public record AssignGradingRequest(
    @NotNull(message = "A grading round must be selected")
    GradingRoundType roundType,

    Instant deadlineAt,

    @NotEmpty(message = "At least one result must be selected for assignment")
    @Valid
    List<AssignmentItemRequest> assignments
) {
    public record AssignmentItemRequest(
        @NotNull(message = "The result to assign is missing")
        UUID candidateResultId,

        @NotNull(message = "The teacher to assign is missing")
        UUID teacherId
    ) {
    }

    public static AssignGradingCommand toCommand(AssignGradingRequest request) {
        return new AssignGradingCommand(
            request.roundType(),
            request.deadlineAt(),
            request.assignments() == null ? List.of() : request.assignments().stream()
                .map(item -> new AssignGradingCommand.AssignmentItem(item.candidateResultId(), item.teacherId()))
                .toList()
        );
    }
}
