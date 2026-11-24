package com.sep.vox.application.port.input.command.schooldirectory;

import java.util.Map;
import java.util.UUID;

public record AcceptSchoolDirectoryImportCommand(
    UUID importSessionId,
    Map<String, String> confirmedMapping
) {
}
