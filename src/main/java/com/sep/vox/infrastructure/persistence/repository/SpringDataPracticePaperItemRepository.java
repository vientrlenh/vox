package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.PracticePaperItemJpaEntity;

public interface SpringDataPracticePaperItemRepository
        extends JpaRepository<PracticePaperItemJpaEntity, UUID> {

    @Query(value = """
        SELECT COALESCE(SUM(
            question.preparation_time_seconds + question.max_response_seconds + question.max_followup_seconds
        ), 0)
        FROM practice_paper_item item
        JOIN practice_question question ON question.id = item.practice_question_id
        WHERE item.practice_paper_id = :paperId
        """, nativeQuery = true)
    int sumPlannedSecondsForPaper(@Param("paperId") UUID paperId);

    @Query(value = """
        SELECT practice_question_id
        FROM practice_paper_item
        WHERE practice_paper_id = :paperId
        ORDER BY slot_order
        """, nativeQuery = true)
    List<UUID> findQuestionIdsForPaper(@Param("paperId") UUID paperId);

    int countByPracticePaperId(UUID paperId);
}
