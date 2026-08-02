package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LearnerProfile(
    UUID id,
    UUID studentId,
    int version,
    String goalType,
    String targetExam,
    LocalDate targetDate,
    BigDecimal flsaScore,
    String flsaRawAnswersJson,
    boolean autoUpdateInterest,
    OffsetDateTime quizCompletedAt,
    OffsetDateTime recordedAt
) {
    public static LearnerProfile first(UUID studentId) {
        return new LearnerProfile(
            null,
            studentId,
            1,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            OffsetDateTime.now()
        );
    }

    public LearnerProfile next(
            String nextGoalType,
            BigDecimal nextFlsaScore,
            String nextFlsaRawAnswersJson,
            Boolean nextAutoUpdate,
            OffsetDateTime nextQuizCompletedAt) {
        return new LearnerProfile(
            null,
            studentId,
            version + 1,
            nextGoalType != null ? nextGoalType : goalType,
            targetExam,
            targetDate,
            nextFlsaScore != null ? nextFlsaScore : flsaScore,
            nextFlsaRawAnswersJson != null
                ? nextFlsaRawAnswersJson
                : flsaRawAnswersJson,
            nextAutoUpdate != null ? nextAutoUpdate : autoUpdateInterest,
            nextQuizCompletedAt != null
                ? nextQuizCompletedAt
                : quizCompletedAt,
            OffsetDateTime.now()
        );
    }
}
