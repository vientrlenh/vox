package com.sep.vox.application.port.input.command;

import com.sep.vox.application.common.UploadedFile;
import java.util.UUID;

public record PreviewFrameworkVersionImportCommand(
        UUID frameworkId,
        UploadedFile file
) {}
