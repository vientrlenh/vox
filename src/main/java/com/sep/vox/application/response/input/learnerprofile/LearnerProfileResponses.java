package com.sep.vox.application.response.input.learnerprofile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LearnerProfileResponses {

    private LearnerProfileResponses() {
    }

    public record LearnerProfile(
        String goalType,
        String targetFrameworkBandCode,
        String targetFrameworkBandLabel,
                boolean interestAutoUpdateEnabled,
        Instant quizCompletedAt
    ) {
    }

    public record InterestQuizItem(UUID id, List<String> statements) {
    }

    /**
     * A band the student can choose as the difficulty of a practice session. DELIBERATELY has no "your band" flag:
     * the system does not rank proficiency levels, it only records the difficulty the student chose in each session.
     */
    public record PracticeBandOption(
        UUID id,
        String code,
        String label,
        String description,
        int order
    ) {
    }

    /**
     * An assessment framework for the student to choose BEFORE choosing a band.
     *
     * <p>{@code versionId} is the latest published version of the framework, chosen by the server itself -- the student
     * only sees the framework name and does not have to understand the concept of a "version". The band chosen afterwards belongs to exactly this
     * version, and because practice grading derives the framework from the target band itself
     * ({@code SpringDataPracticeSessionRepository.findCriteriaFrameworks}), the whole chain is self-consistent.
     */
    public record PracticeFrameworkOption(
        UUID versionId,
        String code,
        String name,
        String description,
        int bandCount
    ) {
    }
}
