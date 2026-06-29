package com.sep.vox.application.port.input.command;

import java.util.Map;
import java.util.UUID;

public record AcceptRubricCriterionBandImportCommand(
        UUID schoolId,
        UUID sessionId,
        Map<String, String> confirmedMapping

) {
}