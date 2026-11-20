package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One proctoring alert, for the grading screen and for the history of the live proctoring screen.
 *
 * <p>{@code capturedAt} is ABSOLUTE time, not a seek position in the video. Mapping it to an offset needs
 * the start time of the recording, which is not stored anywhere yet -- see the note in
 * {@code ViewAssessmentSessionProctoringAlertsUseCase}.
 *
 * <p>{@code candidateId} can be null: the emitting source does not always know which candidate, and here
 * leaving it empty is considered more honest than guessing.
 */
public record AssessmentProctoringAlertDto(
    UUID id,
    String eventId,
    UUID assessmentSessionId,
    UUID candidateId,
    String streamId,
    String streamType,
    String alertType,
    String level,
    String source,
    String detail,
    BigDecimal confidence,
    String capturedAt,
    String raisedAt
) {
}
