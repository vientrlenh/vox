package com.sep.vox.application.event.dummy;

public record DummyReportGeneratedExternalEvent(
    String reportId,
    String requestedBy,
    String status
) {
}
