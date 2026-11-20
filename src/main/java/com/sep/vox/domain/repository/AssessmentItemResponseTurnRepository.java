package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;

public interface AssessmentItemResponseTurnRepository {
    record SessionFollowupCount(UUID assessmentItemResponseId, long followupCount, long totalTurns) {
    }

    AssessmentItemResponseTurn upsert(AssessmentItemResponseTurn turn);
    List<AssessmentItemResponseTurn> findByAssessmentItemResponseId(UUID assessmentItemResponseId);
    Optional<AssessmentItemResponseTurn> findByAssessmentItemResponseIdAndTurnOrder(UUID assessmentItemResponseId, int turnOrder);
    Optional<AssessmentItemResponseTurn> findLatestByAssessmentItemResponseId(UUID assessmentItemResponseId);
    List<SessionFollowupCount> countFollowupsBySessionId(UUID sessionId);
    void deleteByAssessmentItemResponseIdIn(Collection<UUID> assessmentItemResponseIds);
}
