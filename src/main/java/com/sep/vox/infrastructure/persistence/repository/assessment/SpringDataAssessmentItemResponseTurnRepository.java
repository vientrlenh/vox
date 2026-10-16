package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemResponseTurnJpaEntity;

public interface SpringDataAssessmentItemResponseTurnRepository extends JpaRepository<AssessmentItemResponseTurnJpaEntity, UUID> {
    List<AssessmentItemResponseTurnJpaEntity> findByAssessmentItemAnswerIdOrderByTurnOrderAsc(UUID assessmentItemAnswerId);
    Optional<AssessmentItemResponseTurnJpaEntity> findByAssessmentItemAnswerIdAndTurnOrder(UUID assessmentItemAnswerId, int turnOrder);
    Optional<AssessmentItemResponseTurnJpaEntity> findTopByAssessmentItemAnswerIdOrderByTurnOrderDesc(UUID assessmentItemAnswerId);
    void deleteByAssessmentItemAnswerIdIn(java.util.Collection<UUID> assessmentItemAnswerIds);

    interface SessionFollowupCountProjection {
        UUID getAssessmentItemAnswerId();
        long getFollowupCount();
        long getTotalTurns();
    }

    @Query(value = """
        SELECT
            t.assessment_item_answer_id AS assessmentItemAnswerId,
            SUM(CASE WHEN t.turn_type = 'FOLLOWUP' THEN 1 ELSE 0 END) AS followupCount,
            COUNT(*) AS totalTurns
        FROM assessment_item_answer_turns t
        JOIN assessment_item_answers r ON r.id = t.assessment_item_answer_id
        WHERE r.session_id = :sessionId
        GROUP BY t.assessment_item_answer_id
        """, nativeQuery = true)
    List<SessionFollowupCountProjection> countFollowupsBySessionId(@Param("sessionId") UUID sessionId);
}
