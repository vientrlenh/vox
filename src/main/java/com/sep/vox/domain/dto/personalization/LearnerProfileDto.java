package com.sep.vox.domain.dto.personalization;

public record LearnerProfileDto(
    String goalType,
    Double flsaScore,
    String targetFrameworkBandCode,
    String targetFrameworkBandLabel,
    Double targetBandAttainmentPercent,
    String estimatedFrameworkBandCode,
    boolean interestAutoUpdateEnabled,
    String quizCompletedAt
) {
}
