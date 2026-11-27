package com.sep.vox.application.response.input.assessment;

import java.util.List;
import java.util.UUID;

public record StudentAssessmentPaperResponse(
    UUID assessmentId,
    UUID assessmentPaperId,
    String title,
    String subject,
    String description,
    int durationSeconds,
    int durationMinutes,
    String assessmentDate,
    String status,
    String scheduleEndAt,
    String startedAt,
    /**
     * The countdown clock checkpoint, null if the session has never checkpointed -- the client reads that as
     * "count from durationSeconds". It cannot be derived from startedAt because the clock stops while the avatar speaks.
     */
    Integer remainingSeconds,
    List<StudentAssessmentPaperQuestionResponse> paperQuestions
) {
}
