package com.sep.vox.application.query.dto;

public record LearnerProfileInfo(
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
