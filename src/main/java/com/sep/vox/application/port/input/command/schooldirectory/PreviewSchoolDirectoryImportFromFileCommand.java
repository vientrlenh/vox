package com.sep.vox.application.port.input.command.schooldirectory;

import com.sep.vox.application.common.UploadedFile;

public record PreviewSchoolDirectoryImportFromFileCommand(
    UploadedFile file
) {
}
