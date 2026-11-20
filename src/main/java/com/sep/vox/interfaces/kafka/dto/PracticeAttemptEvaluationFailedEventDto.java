package com.sep.vox.interfaces.kafka.dto;

/**
 * The agents report that grading a practice answer did NOT FINISH.
 *
 * <p>The agents have been firing this event for a long time ({@code publish_practice_attempt_evaluation_failed}),
 * but before 2026-08-12 the Java side fell into the branch "Skip unknown practice evaluation event type"
 * and dropped it outright. Consequence: when grading failed nobody knew, that answer was permanently "not graded",
 * the sweep job re-fired every 5 minutes, grading failed again -- an infinite loop, and the summary screen spun forever waiting for
 * a result that would never come. The ASSESSMENT side handled it properly from the start
 * ({@code AssessmentAttemptEvaluationFailed}); only the practice side was missing it.
 *
 * <p>The field names follow exactly what the agents send: {@code practice_event_body} changes
 * assessmentAttemptId/answerId/questionId into the practice* set before publishing.
 */
public record PracticeAttemptEvaluationFailedEventDto(
    String eventType,
    Integer schemaVersion,
    String practiceSessionId,
    String practiceResponseId,
    String practiceQuestionId,
    PayloadDto payload
) {
    public record PayloadDto(
        String error,
        Integer retryCount
    ) {
    }
}
