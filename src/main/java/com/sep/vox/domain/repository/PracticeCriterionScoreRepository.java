package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.personalization.PracticeCriterionScoreDto;

public interface PracticeCriterionScoreRepository {

    /**
     * Writes or updates the score of 1 criterion for 1 grading, identified by the criterion CODE.
     *
     * <p>Since V13 {@code rubric_criterions} is no longer looked up to get the id: practice is graded on a fixed
     * 0-100 scale, belonging to no rubric. The old version looked up the rubric and did {@code continue} when nothing
     * matched -- meaning the score vanished without a trace, exactly how the 2026-08-06 bug on the assessment path stayed hidden.
     *
     * <p>Does not take {@code matchedBandCode}: practice grading compares against EXACTLY ONE band the student
     * chose and does not classify, so there is no band to assign.
     */
    void upsertByCode(
        UUID practiceEvaluationId,
        String criterionCode,
        double finalScore
    );

    List<PracticeCriterionScoreDto> findScoresBySessionId(UUID sessionId);
}
