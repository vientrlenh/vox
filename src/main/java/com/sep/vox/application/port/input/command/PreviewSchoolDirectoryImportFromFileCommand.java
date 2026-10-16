package com.sep.vox.application.port.input.command;

import com.sep.vox.application.shared.UploadedFile;

public record PreviewSchoolDirectoryImportFromFileCommand(
    UploadedFile file
) {
}
