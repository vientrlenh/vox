package com.sep.vox.application.event.assessment;

import com.sep.vox.application.event.ExternalEventTopic;

@ExternalEventTopic("assessment-attempt-force-end-requested")
public record AssessmentAttemptForceEndRequestedExternalEvent(
    String eventType,
    Integer schemaVersion,
    String assessmentAttemptId,
    Payload payload
) {
    public AssessmentAttemptForceEndRequestedExternalEvent(String assessmentAttemptId, String reason) {
        this("AssessmentAttemptForceEndRequested", 1, assessmentAttemptId, new Payload(reason));
    }

    public record Payload(String reason) {
    }
}
