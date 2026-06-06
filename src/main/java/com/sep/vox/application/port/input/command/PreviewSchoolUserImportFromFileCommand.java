package com.sep.vox.application.port.input.command;

import java.util.Map;
import java.util.UUID;

public record PreviewSchoolUserImportFromFileCommand(
    UUID schoolId,
    String fileId,
    String defaultRole,
    Map<String, ImportFieldMapping> mapping
) {
}
