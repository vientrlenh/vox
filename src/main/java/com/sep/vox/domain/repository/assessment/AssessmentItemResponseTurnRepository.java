package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;

public interface AssessmentItemResponseTurnRepository {
    record SessionFollowupCount(UUID examItemResponseId, long followupCount, long totalTurns) {
    }

    AssessmentItemResponseTurn upsert(AssessmentItemResponseTurn turn);
    List<AssessmentItemResponseTurn> findByExamItemResponseId(UUID examItemResponseId);
    Optional<AssessmentItemResponseTurn> findByExamItemResponseIdAndTurnOrder(UUID examItemResponseId, int turnOrder);
    Optional<AssessmentItemResponseTurn> findLatestByExamItemResponseId(UUID examItemResponseId);
    List<SessionFollowupCount> countFollowupsBySessionId(UUID sessionId);
    void deleteByExamItemResponseIdIn(Collection<UUID> examItemResponseIds);
}
