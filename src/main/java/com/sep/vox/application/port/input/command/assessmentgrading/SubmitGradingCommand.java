package com.sep.vox.application.port.input.command.assessmentgrading;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A teacher submits scores for the exam parts of a submission. The same command serves both
 * {@code /regrade} (write & finalize) and {@code /regrade/preview} (compute only) -- the two
 * paths must receive exactly the same input so that the totals they return are equal.
 *
 * <p>Has NO total score: the total is always re-derived from the criterion scores.
 *
 * <p>Enters by {@code assignmentId}, not {@code candidateResultId}: the permission to grade
 * comes from the assignment row itself, and the grading round ({@code roundType}) lives there too.
 */
public record SubmitGradingCommand(
    UUID assignmentId,
    List<ItemGrade> items
) {
    public record ItemGrade(
        UUID paperItemId,
        List<CriterionScoreItem> criterionScores,
        String feedbackSummary
    ) {
    }

    public record CriterionScoreItem(
        UUID rubricCriterionId,
        BigDecimal score,
        String rationale
    ) {
    }
}
