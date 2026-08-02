package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.personalization.PracticeCriterionScoreDto;

public interface PracticeCriterionScoreRepository {

    /** Ghi hoặc cập nhật điểm 1 tiêu chí cho 1 evaluation. */
    void upsert(
        UUID practiceEvaluationId,
        UUID rubricCriterionId,
        double finalScore,
        String matchedBandCode
    );

    List<PracticeCriterionScoreDto> findScoresBySessionId(UUID sessionId);
}
