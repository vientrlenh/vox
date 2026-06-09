package com.sep.vox.domain.dto;

import java.util.List;
import java.util.UUID;

public record QuestionDetailDto(
    UUID id,
    UUID questionTopicId,
    String code,
    String instructionText,
    String questionText,
    String promptText,
    String preparationText,
    String type,
    int preparationTimeSeconds,
    int minResponseSeconds,
    int maxResponseSeconds,
    String scope,
    String visibility,
    UUID sourceQuestionId,
    boolean locked,
    String status,
    String createdAt,
    String updatedAt,
    QuestionTopicDto questionTopic,
    QuestionEvaluationGuideDto evaluationGuide,
    List<QuestionAssetDto> assets
) {
}
