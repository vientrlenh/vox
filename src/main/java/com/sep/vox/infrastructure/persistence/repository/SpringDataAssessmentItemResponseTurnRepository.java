package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.AssessmentItemResponseTurnJpaEntity;

public interface SpringDataAssessmentItemResponseTurnRepository extends JpaRepository<AssessmentItemResponseTurnJpaEntity, UUID> {
    List<AssessmentItemResponseTurnJpaEntity> findByAssessmentItemResponseIdOrderByTurnOrderAsc(UUID assessmentItemResponseId);
    Optional<AssessmentItemResponseTurnJpaEntity> findByAssessmentItemResponseIdAndTurnOrder(UUID assessmentItemResponseId, int turnOrder);
    Optional<AssessmentItemResponseTurnJpaEntity> findTopByAssessmentItemResponseIdOrderByTurnOrderDesc(UUID assessmentItemResponseId);
    void deleteByAssessmentItemResponseIdIn(java.util.Collection<UUID> assessmentItemResponseIds);

    interface SessionFollowupCountProjection {
        UUID getAssessmentItemResponseId();
        long getFollowupCount();
        long getTotalTurns();
    }

    @Query(value = """
        SELECT
            t.assessment_item_response_id AS assessmentItemResponseId,
            SUM(CASE WHEN t.turn_type = 'FOLLOWUP' THEN 1 ELSE 0 END) AS followupCount,
            COUNT(*) AS totalTurns
        FROM assessment_item_response_turns t
        JOIN assessment_item_responses r ON r.id = t.assessment_item_response_id
        WHERE r.session_id = :sessionId
        GROUP BY t.assessment_item_response_id
        """, nativeQuery = true)
    List<SessionFollowupCountProjection> countFollowupsBySessionId(@Param("sessionId") UUID sessionId);
}
