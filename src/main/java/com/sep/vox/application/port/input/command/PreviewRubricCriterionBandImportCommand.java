package com.sep.vox.application.port.input.command;

import com.sep.vox.application.common.UploadedFile;
import java.util.UUID;

public record PreviewRubricCriterionBandImportCommand(
        UUID schoolId, // Sẽ là null nếu là System Admin
        UUID criterionId,
        UploadedFile file
) {
}