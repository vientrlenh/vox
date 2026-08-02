package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PracticeQuestion(
    UUID id,
    UUID practiceTopicId,
    String questionText,
    String targetCriterionCode,
    String targetSubAttribute,
    int difficultyRank,
    String difficultyFeaturesJson,
    String evaluationGuideJson,
    String suggestedIdeasJson,
    int preparationTimeSeconds,
    int maxResponseSeconds,
    int maxFollowupSeconds,
    Integer vstepPart,
    String source,
    int usageCount,
    boolean active,
    OffsetDateTime createdAt
) {
    public int spokenSeconds() {
        return maxResponseSeconds + maxFollowupSeconds;
    }

    public int plannedSeconds() {
        return preparationTimeSeconds + spokenSeconds();
    }
}
