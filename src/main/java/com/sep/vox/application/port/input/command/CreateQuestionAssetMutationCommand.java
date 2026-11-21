package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionAssetType;

public record CreateQuestionAssetMutationCommand(
    UUID questionId,
    String title,
    Integer durationSeconds,
    String altText,
    QuestionAssetType type,
    String url,
    String transcript,
    String description,
    int order
) {
}
