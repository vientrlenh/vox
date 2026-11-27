package com.sep.vox.application.port.input.command.rubricsystem;

import com.sep.vox.application.common.UploadedFile;
import java.util.UUID;

public record PreviewRubricCriterionImportCommand(
        UUID schoolId, // null for a System Admin
        UUID rubricVersionId,
        UploadedFile file
) {}