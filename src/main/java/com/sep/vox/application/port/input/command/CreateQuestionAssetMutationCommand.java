package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateQuestionAssetMutationCommand(
    UUID questionId,
    String title,
    Integer durationSeconds,
    String altText,
    String type,
    String url,
    String transcript,
    String description,
    int order
) {
}
