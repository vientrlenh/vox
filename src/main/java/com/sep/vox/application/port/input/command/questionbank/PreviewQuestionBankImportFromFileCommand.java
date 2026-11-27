package com.sep.vox.application.port.input.command.questionbank;

import com.sep.vox.application.common.UploadedFile;

/**
 * Imports a question bank.
 *
 * <p>No scope parameter: which school the bank belongs to (or the system) is derived in the use case from the role of
 * the logged-in user. Letting the client declare schoolId would open the way for someone to create a
 * bank for another school.
 */
public record PreviewQuestionBankImportFromFileCommand(
    UploadedFile file
) {
}
