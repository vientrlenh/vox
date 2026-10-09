package com.sep.vox.application.port.input.command;

import com.sep.vox.application.shared.UploadedFile;
import java.util.UUID;

public record PreviewSchoolRubricVersionImportCommand(
        UUID schoolId,
        UUID rubricId,
        UploadedFile file
) {}