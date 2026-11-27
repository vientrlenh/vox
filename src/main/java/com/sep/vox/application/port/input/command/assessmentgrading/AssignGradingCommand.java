package com.sep.vox.application.port.input.command.assessmentgrading;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.GradingRoundType;

/**
 * Manually assigns many submissions at once. The batch is deliberate: the admin ticks many rows on the table
 * and assigns in one go, and the "one open assignment per submission" constraint is checked entirely within one
 * transaction instead of being scattered over N requests.
 *
 * @param roundType  the grading round applied to the whole batch. One assignment covers a single round -- mixing rounds
 *                   in one batch makes the submission-status check rules ambiguous, and in the admin UI
 *                   the round is also chosen first before ticking submissions.
 * @param deadlineAt the common grading deadline, may be null
 */
public record AssignGradingCommand(
    GradingRoundType roundType,
    Instant deadlineAt,
    List<AssignmentItem> assignments
) {
    public record AssignmentItem(
        UUID candidateResultId,
        UUID teacherId
    ) {
    }
}
