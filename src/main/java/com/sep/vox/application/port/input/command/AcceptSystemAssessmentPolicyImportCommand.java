package com.sep.vox.application.port.input.command;

import java.util.Map;
import java.util.UUID;

public record AcceptSystemAssessmentPolicyImportCommand(
        UUID sessionId,
        Map<String, String> confirmedMapping
) {}
