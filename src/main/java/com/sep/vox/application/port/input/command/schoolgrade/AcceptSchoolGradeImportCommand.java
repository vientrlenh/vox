package com.sep.vox.application.port.input.command.schoolgrade;

import java.util.Map;
import java.util.UUID;

public record AcceptSchoolGradeImportCommand(
    UUID schoolId,
    UUID importSessionId,
    Map<String, String> confirmedMapping
) {
}
