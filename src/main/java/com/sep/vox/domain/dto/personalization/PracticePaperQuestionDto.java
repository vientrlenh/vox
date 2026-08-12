package com.sep.vox.domain.dto.personalization;

import java.util.List;
import java.util.UUID;

public record PracticePaperQuestionDto(
    UUID questionId,
    int slot,
    String questionText,
    String criterionCode,
    String subAttribute,
    int difficultyRank,
    int maxResponseSeconds,
    int minResponseSeconds,
    List<String> suggestedIdeas
) {
}
