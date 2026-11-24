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
    @NotNull(message = "Phải chọn vòng chấm")
    GradingRoundType roundType,

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
            request.roundType(),
            request.deadlineAt(),
            request.assignments() == null ? List.of() : request.assignments().stream()
                .map(item -> new AssignGradingCommand.AssignmentItem(item.candidateResultId(), item.teacherId()))
                .toList()
        );
    }
}
