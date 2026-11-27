package com.sep.vox.application.response.input.assessmentgrading;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The common result of the four grading actions.
 *
 * <p>One return type for all four so the FE has exactly one handling path after a button click:
 * read {@code resultStatus} + {@code totalScore} and redraw the row, with no branching
 * on which button was pressed.
 *
 * @param nextAssignmentId the assignment just opened next in the same transaction; currently only
 *        {@code CLEAR_INVALID} produces it (opening an {@code INITIAL} round for that same teacher),
 *        {@code null} for the other actions
 */
public record GradingActionResponse(
    UUID assignmentId,
    UUID candidateResultId,
    String outcome,
    String resultStatus,
    BigDecimal totalScore,
    UUID nextAssignmentId
) {
}
