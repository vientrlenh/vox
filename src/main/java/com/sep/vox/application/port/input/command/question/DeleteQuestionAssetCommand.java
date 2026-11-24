package com.sep.vox.application.port.input.command.question;

import java.util.UUID;

public record DeleteQuestionAssetCommand(
    UUID questionId,
    UUID assetId
) {
}
