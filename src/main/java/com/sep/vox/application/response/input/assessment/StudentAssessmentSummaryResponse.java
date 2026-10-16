package com.sep.vox.application.response.input.assessment;

import java.util.List;
import java.util.UUID;

public record StudentAssessmentSummaryResponse(
    UUID id,
    String title,
    String subject,
    String description,
    int duration,
    String assessmentDate,
    String status,
    String kind,
    boolean requiresOtp,
    List<StudentAssessmentSessionSummaryResponse> sessions,
    Integer maxAttempt,
    int attemptsUsed,
    boolean canEnter,
    String entryMessage
) {
}
