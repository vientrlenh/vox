package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.application.common.UploadedFile;

public record PreviewSchoolRoomImportFromFileCommand(
    UUID schoolId,
    UploadedFile file
) {
}
