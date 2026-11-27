package com.sep.vox.interfaces.rest.dto.request.assessmentgrading;

import com.sep.vox.application.port.input.command.assessmentgrading.SubmitGradingCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Shared body for {@code /grade} and {@code /grade/preview} -- both paths must
 * accept exactly the same input shape so the totals they return are equal.
 *
 * <p>There is no total score field: the backend recomputes the total from the criterion scores.
 * The min/max range of each criterion comes from {@code gradingTaskDetail}, and the backend validates it again.
 */
public record SubmitGradingRequest(
    @NotEmpty(message = "At least one section must be scored")
    @Valid
    List<ItemGradeRequest> items
) {
    public record ItemGradeRequest(
        @NotNull(message = "The section to grade is missing")
        UUID paperItemId,

        @NotEmpty(message = "The criteria must be scored")
        @Valid
        List<CriterionScoreRequest> criterionScores,

        @Size(max = 2048, message = "Comment must not exceed 2048 characters")
        String feedbackSummary
    ) {
    }

    public record CriterionScoreRequest(
        @NotNull(message = "The scoring criterion is missing")
        UUID rubricCriterionId,

        @NotNull(message = "The criterion must be scored")
        BigDecimal score,

        @Size(max = 512, message = "Explanation must not exceed 512 characters")
        String rationale
    ) {
    }

    /** Used for both /regrade and /regrade/preview: same body, same command. */
    public static SubmitGradingCommand toCommand(UUID assignmentId, SubmitGradingRequest request) {
        return new SubmitGradingCommand(assignmentId, toItemGrades(request));
    }

    private static List<SubmitGradingCommand.ItemGrade> toItemGrades(SubmitGradingRequest request) {
        return request.items() == null ? List.of() : request.items().stream()
            .map(item -> new SubmitGradingCommand.ItemGrade(
                item.paperItemId(),
                item.criterionScores() == null ? List.of() : item.criterionScores().stream()
                    .map(score -> new SubmitGradingCommand.CriterionScoreItem(
                        score.rubricCriterionId(), score.score(), score.rationale()))
                    .toList(),
                item.feedbackSummary()
            ))
            .toList();
    }
}
