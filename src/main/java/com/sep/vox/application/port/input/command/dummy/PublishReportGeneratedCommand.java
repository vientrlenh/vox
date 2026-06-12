package com.sep.vox.application.port.input.command.dummy;

public record PublishReportGeneratedCommand(
    String reportId,
    String requestedBy,
    String status
) {}
