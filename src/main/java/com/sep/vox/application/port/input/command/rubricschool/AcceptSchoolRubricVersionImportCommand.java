package com.sep.vox.application.port.input.command.rubricschool;

import java.util.Map;
import java.util.UUID;

public record AcceptSchoolRubricVersionImportCommand(
        UUID schoolId,
        UUID sessionId,
        Map<String, String>confirmedMapping
) {}