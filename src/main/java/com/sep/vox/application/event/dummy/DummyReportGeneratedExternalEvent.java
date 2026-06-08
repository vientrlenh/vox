package com.sep.vox.application.event.dummy;

import com.sep.vox.application.event.ExternalEventTopic;

@ExternalEventTopic("audit-events")
public record DummyReportGeneratedExternalEvent(
    String reportId,
    String requestedBy,
    String status
) {
}
