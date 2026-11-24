package com.sep.vox.application.port.input.command.schooluser;

import java.util.Map;
import java.util.UUID;

public record AcceptSchoolUserImportCommand(
    UUID schoolId,
    UUID importSessionId,
    Map<String, String> confirmedMapping
) {
}
