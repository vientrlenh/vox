package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateQuestionAssetCommand(
    UUID questionId,
    UUID assetId,
    String title,
    Integer durationSeconds,
    String altText,
    String type,
    String url,
    String transcript,
    String description,
    Integer order
) {
}
