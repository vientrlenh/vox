package com.sep.vox.application.port.input.command;

import java.util.Map;
import java.util.UUID;

public record ImportSchoolUsersCommand(
    UUID schoolId,
    String fileId,
    boolean dryRun,
    String defaultRole,
    Map<String, ImportFieldMapping> mapping
) {
}
