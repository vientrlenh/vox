package com.sep.vox.application.port.input.command.schoolclassuser;

import java.util.UUID;

import com.sep.vox.application.common.UploadedFile;

public record PreviewSchoolClassUserImportFromFileCommand(
    UUID schoolId,
    UploadedFile file
) {
}
