package com.sep.vox.application.query.dto;

public record LearnerProfileInfo(
    String goalType,
    String targetFrameworkBandCode,
    String targetFrameworkBandLabel,
    boolean interestAutoUpdateEnabled,
    String quizCompletedAt
) {
}
