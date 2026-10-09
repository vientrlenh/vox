package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.application.shared.UploadedFile;

public record PreviewQuestionImportFromFileCommand(
    UUID questionBankId,
    UUID questionTopicId,
    UploadedFile file
) {
}
