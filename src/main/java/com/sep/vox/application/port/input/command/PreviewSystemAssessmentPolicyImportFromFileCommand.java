package com.sep.vox.application.port.input.command;

import com.sep.vox.application.shared.UploadedFile;

public record PreviewSystemAssessmentPolicyImportFromFileCommand(
        UploadedFile file
) {}
