package com.sep.vox.application.port.input.command.assessmentgrading;

import java.util.UUID;

/**
 * The common input of the three actions that do not enter a score: {@code UPHOLD}, {@code INVALIDATE},
 * {@code CLEAR_INVALID} and {@code DECLINE}.
 *
 * <p>Those actions differ only in the conclusion, not in the data, so they share
 * one command. Only {@code REGRADE} has its own {@link SubmitGradingCommand} because it carries
 * the whole score sheet.
 *
 * @param reason required for {@code INVALIDATE} / {@code CLEAR_INVALID} / {@code DECLINE};
 *               optional for {@code UPHOLD} (the teacher's note)
 */
public record GradingDecisionCommand(
    UUID assignmentId,
    String reason
) {
}
