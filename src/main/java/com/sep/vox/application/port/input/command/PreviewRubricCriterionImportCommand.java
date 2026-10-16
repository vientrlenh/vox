package com.sep.vox.application.port.input.command;

import com.sep.vox.application.shared.UploadedFile;
import java.util.UUID;

public record PreviewRubricCriterionImportCommand(
        UUID schoolId, // Sẽ là null nếu là System Admin
        UUID rubricVersionId,
        UploadedFile file
) {}