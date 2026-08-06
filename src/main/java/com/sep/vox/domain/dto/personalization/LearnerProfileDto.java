package com.sep.vox.domain.dto.personalization;

public record LearnerProfileDto(
    String goalType,
    Double flsaScore,
    String targetFrameworkBandCode,
    String targetFrameworkBandLabel,
    boolean interestAutoUpdateEnabled,
    String quizCompletedAt
) {
}
