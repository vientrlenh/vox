package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamItemResponseTurnJpaEntity;

public interface SpringDataExamItemResponseTurnRepository extends JpaRepository<ExamItemResponseTurnJpaEntity, UUID> {
    List<ExamItemResponseTurnJpaEntity> findByExamItemResponseIdOrderByTurnOrderAsc(UUID examItemResponseId);
    Optional<ExamItemResponseTurnJpaEntity> findByExamItemResponseIdAndTurnOrder(UUID examItemResponseId, int turnOrder);
    Optional<ExamItemResponseTurnJpaEntity> findTopByExamItemResponseIdOrderByTurnOrderDesc(UUID examItemResponseId);
    void deleteByExamItemResponseIdIn(java.util.Collection<UUID> examItemResponseIds);

    interface SessionFollowupCountProjection {
        UUID getExamItemResponseId();
        long getFollowupCount();
        long getTotalTurns();
    }

    @Query(value = """
        SELECT
            t.exam_item_response_id AS examItemResponseId,
            SUM(CASE WHEN t.turn_type = 'FOLLOWUP' THEN 1 ELSE 0 END) AS followupCount,
            COUNT(*) AS totalTurns
        FROM exam_item_response_turns t
        JOIN exam_item_responses r ON r.id = t.exam_item_response_id
        WHERE r.session_id = :sessionId
        GROUP BY t.exam_item_response_id
        """, nativeQuery = true)
    List<SessionFollowupCountProjection> countFollowupsBySessionId(@Param("sessionId") UUID sessionId);
}
