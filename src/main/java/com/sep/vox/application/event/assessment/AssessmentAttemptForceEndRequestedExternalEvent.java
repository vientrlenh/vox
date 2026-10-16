package com.sep.vox.application.event.assessment;

import com.sep.vox.application.event.ExternalEventTopic;

@ExternalEventTopic("exam-attempt-force-end-requested")
public record AssessmentAttemptForceEndRequestedExternalEvent(
    String eventType,
    Integer schemaVersion,
    String examAttemptId,
    Payload payload
) {
    public AssessmentAttemptForceEndRequestedExternalEvent(String examAttemptId, String reason) {
        this("AssessmentAttemptForceEndRequested", 1, examAttemptId, new Payload(reason));
    }

    public record Payload(String reason) {
    }
}
