package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.PracticeItemResponseJpaEntity;

public interface SpringDataPracticeItemResponseRepository
        extends JpaRepository<PracticeItemResponseJpaEntity, UUID> {

    Optional<PracticeItemResponseJpaEntity> findByPracticeSessionIdAndPracticeQuestionId(
        UUID practiceSessionId,
        UUID practiceQuestionId
    );

    List<PracticeItemResponseJpaEntity> findByPracticeSessionId(UUID practiceSessionId);

    @Query(value = """
        SELECT session.rubric_version_id
        FROM practice_item_response response
        JOIN practice_session session ON session.id = response.practice_session_id
        WHERE response.id = :responseId
        """, nativeQuery = true)
    UUID findRubricVersionIdByResponseId(@Param("responseId") UUID responseId);
}
