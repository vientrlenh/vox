package com.sep.vox.application.query.dto;

import java.time.Instant;

public record LearnerProfileInfo(
    String goalType,
    String targetFrameworkBandCode,
    String targetFrameworkBandLabel,
    boolean interestAutoUpdateEnabled,
    Instant quizCompletedAt
) {
}
