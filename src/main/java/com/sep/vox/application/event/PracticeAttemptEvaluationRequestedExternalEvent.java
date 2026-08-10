package com.sep.vox.application.event;

import com.sep.vox.application.event.ExamAttemptEvaluationRequestedExternalEvent.Payload;

@ExternalEventTopic("practice-attempt-evaluation-requested")
public record PracticeAttemptEvaluationRequestedExternalEvent(
    String eventType,
    Integer schemaVersion,
    String practiceSessionId,
    String practiceResponseId,
    String practiceQuestionId,
    Payload payload
) implements HasPartitionKey {

    public PracticeAttemptEvaluationRequestedExternalEvent(
            String practiceSessionId,
            String practiceResponseId,
            String practiceQuestionId,
            Payload payload) {
        this(
            "PracticeAttemptEvaluationRequested",
            1,
            practiceSessionId,
            practiceResponseId,
            practiceQuestionId,
            payload
        );
    }

    @Override
    public String partitionKey() {
        return practiceSessionId;
    }
}
