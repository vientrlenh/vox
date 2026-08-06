package com.sep.vox.application.port.input.command;

import com.sep.vox.application.common.UploadedFile;
import java.util.UUID;

public record PreviewFrameworkCriterionBandImportCommand(
        UUID versionId,
        UploadedFile file
) {}
