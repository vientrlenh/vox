package com.sep.vox.application.response.input.learnerprofile;

import java.util.List;
import java.util.UUID;

public final class LearnerProfileResponses {

    private LearnerProfileResponses() {
    }

    public record LearnerProfile(
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

    public record InterestQuizItem(UUID id, List<String> statements) {
    }
}
