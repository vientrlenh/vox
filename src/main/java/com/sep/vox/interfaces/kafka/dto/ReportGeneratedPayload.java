package com.sep.vox.interfaces.kafka.dto;

public record ReportGeneratedPayload(
    String reportId,
    String requestedBy,
    String status
) {
    
}
