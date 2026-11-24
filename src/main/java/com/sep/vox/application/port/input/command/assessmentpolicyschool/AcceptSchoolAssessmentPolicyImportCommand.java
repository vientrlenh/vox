package com.sep.vox.application.port.input.command.assessmentpolicyschool;

import java.util.Map;
import java.util.UUID;

public record AcceptSchoolAssessmentPolicyImportCommand(
        UUID schoolId,
        UUID sessionId,
        Map<String, String> confirmedMapping
) {}
