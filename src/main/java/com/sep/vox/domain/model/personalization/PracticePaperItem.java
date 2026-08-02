package com.sep.vox.domain.model.personalization;

import java.util.UUID;

public record PracticePaperItem(
    UUID id,
    UUID practicePaperId,
    UUID practiceQuestionId,
    int slotOrder,
    String targetCriterionCode,
    String targetSubAttribute,
    int targetDifficultyRank
) {
}
