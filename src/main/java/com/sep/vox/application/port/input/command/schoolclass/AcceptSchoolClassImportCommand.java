package com.sep.vox.application.port.input.command.schoolclass;

import java.util.Map;
import java.util.UUID;

public record AcceptSchoolClassImportCommand(
    UUID schoolId,
    UUID importSessionId,
    Map<String, String> confirmedMapping
) {
}
