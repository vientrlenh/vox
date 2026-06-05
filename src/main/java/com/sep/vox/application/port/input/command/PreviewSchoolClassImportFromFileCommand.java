package com.sep.vox.application.port.input.command;

import com.sep.vox.application.common.UploadedFile;

public record PreviewSchoolClassImportFromFileCommand(
    UploadedFile file
) {
    
}
