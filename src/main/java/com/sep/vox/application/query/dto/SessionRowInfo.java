package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

public interface SessionRowInfo {

    UUID getId();

    UUID getPracticePaperId();

    UUID getChosenPracticeTopicId();

    String getTopicName();

    String getOrigin();

    String getStatus();

    String getAbandonDiagnosis();

    Double getOverallScore();

    int getGradedSeconds();

    String getOfferedTopicIdsJson();

    // Instant rather than OffsetDateTime: the driver returns TIMESTAMPTZ as Instant, and a
    // Spring Data projection has NO Instant -> OffsetDateTime converter, so it would throw
    // UnsupportedOperationException as soon as the first row of data appears.
    Instant getStartedAt();

    Instant getEndedAt();

    /**
     * The number of questions the student HAS SPOKEN that have no grade yet.
     *
     * <p>Grading is asynchronous, so {@code overall_score} at session close only combines the questions
     * that have finished grading. This number tells the client whether the score it holds is PROVISIONAL or
     * final -- without it, the list shows a number that will change after a few dozen seconds without any warning.
     */
    int getPendingEvaluations();

    /**
     * The score scale of this very session. Sessions from V13 on are always 0-100; OLD sessions take the
     * scale of the rubric used at the time. Derived per session rather than from a global config -- both
     * kinds coexist long term and the summary screen must display both correctly.
     */
    double getScoreScaleMin();

    double getScoreScaleMax();
}
