package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * The score is published to the student for the first time (the {@code INITIAL} round has ended).
 *
 * <p>Kept apart from {@link AssessmentResultRegradedPayloadV1}: the first one is the "your score is available" message, while a
 * score change after publishing is the "your score has just changed" message -- two very different emails.
 *
 * @param sessionId the student's result screen takes sessionId rather than
 *        candidateResultId, so without it the notification can only open the assessment list
 * @param assessmentKind centralized assessments and class tests have two separate result screens
 */
public record AssessmentResultReleasedPayloadV1(
    UUID candidateResultId,
    UUID studentId,
    String assessmentName,
    BigDecimal totalScore,
    UUID sessionId,
    AssessmentKind assessmentKind
) {

}
