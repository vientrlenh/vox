package com.sep.vox.application.port.input.command.assessmentpolicysystem;

import com.sep.vox.application.common.UploadedFile;

public record PreviewSystemAssessmentPolicyImportFromFileCommand(
        UploadedFile file
) {}
