package com.sep.vox.application.port.input.command.assessmentgrading;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.GradingSampleSelectionMode;

/**
 * Automatic assignment within one assessment or one schedule, for one grading round.
 *
 * @param roundType     the round to assign; decides which submissions are eligible
 * @param selectionMode how to choose submissions from the eligible set (see
 *                      {@code GradingSampleSelectionMode})
 * @param percent       only used for {@code RANDOM_PERCENT} and (optionally)
 *                      {@code RISK_BASED}
 * @param candidateResultIds only used for {@code MANUAL_LIST}
 */
public record AutoAssignGradingCommand(
    UUID assessmentId,
    UUID scheduleId,
    GradingRoundType roundType,
    GradingSampleSelectionMode selectionMode,
    Integer percent,
    List<UUID> candidateResultIds,
    Instant deadlineAt,
    List<UUID> teacherIds
) {
}
