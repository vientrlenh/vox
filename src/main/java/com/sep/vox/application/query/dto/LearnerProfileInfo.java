package com.sep.vox.application.query.dto;

public record LearnerProfileInfo(
    String goalType,
    Double flsaScore,
    String targetFrameworkBandCode,
    String targetFrameworkBandLabel,
    boolean interestAutoUpdateEnabled,
    String quizCompletedAt
) {
}
