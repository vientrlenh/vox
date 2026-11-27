package com.sep.vox.application.port.input.command.assessmentgrading;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.GradingRoundType;

/**
 * The teacher who created a class test claims the grading of a batch of submissions themselves.
 *
 * <p>The {@code INITIAL} round is usually opened automatically; this command is for the other two
 * rounds ({@code SPOT_CHECK} re-examining a published submission, {@code REMEDIATION} re-examining a submission
 * that was invalidated), and for the case where an {@code INITIAL} submission has no assignment for some reason.
 */
public record ClaimClassTestGradingCommand(
    UUID assessmentId,
    GradingRoundType roundType,
    List<UUID> candidateResultIds
) {
}
