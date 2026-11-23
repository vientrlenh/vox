package com.sep.vox.application.port.input.command.schoolclass;

import java.util.UUID;

import com.sep.vox.application.common.UploadedFile;

public record PreviewSchoolClassImportFromFileCommand(
    UUID schoolId,
    UploadedFile file
) {
    
}
