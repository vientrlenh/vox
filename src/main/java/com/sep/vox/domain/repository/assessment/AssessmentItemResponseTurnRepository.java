package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentItemAnswerTurn;

public interface AssessmentItemResponseTurnRepository {
    record SessionFollowupCount(UUID assessmentItemAnswerId, long followupCount, long totalTurns) {
    }

    AssessmentItemAnswerTurn upsert(AssessmentItemAnswerTurn turn);
    List<AssessmentItemAnswerTurn> findByAssessmentItemAnswerId(UUID assessmentItemAnswerId);
    Optional<AssessmentItemAnswerTurn> findByAssessmentItemAnswerIdAndTurnOrder(UUID assessmentItemAnswerId, int turnOrder);
    Optional<AssessmentItemAnswerTurn> findLatestByAssessmentItemAnswerId(UUID assessmentItemAnswerId);
    List<SessionFollowupCount> countFollowupsBySessionId(UUID sessionId);
    void deleteByAssessmentItemAnswerIdIn(Collection<UUID> assessmentItemAnswerIds);
}
