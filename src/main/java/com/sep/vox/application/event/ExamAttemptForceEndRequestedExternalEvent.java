package com.sep.vox.application.event;

@ExternalEventTopic("exam-attempt-force-end-requested")
public record ExamAttemptForceEndRequestedExternalEvent(
    String eventType,
    Integer schemaVersion,
    String examAttemptId,
    Payload payload
) {
    public ExamAttemptForceEndRequestedExternalEvent(String examAttemptId, String reason) {
        this("ExamAttemptForceEndRequested", 1, examAttemptId, new Payload(reason));
    }

    public record Payload(String reason) {
    }
}
