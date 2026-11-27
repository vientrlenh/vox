package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.personalization.PracticePaperItemJpaEntity;

public interface SpringDataPracticePaperItemRepository
        extends JpaRepository<PracticePaperItemJpaEntity, UUID> {

    @Query(value = """
        -- ESTIMATED budget of the paper = sum of the speaking CEILINGS of the questions. min_response_seconds is not added
        -- (it is a floor to know when an answer is long enough, not an extra expense); the two old columns
        -- max_followup_seconds and preparation_time_seconds were removed -- see V11.
        SELECT COALESCE(SUM(question.max_response_seconds), 0)
        FROM practice_paper_items item
        JOIN practice_questions question ON question.id = item.practice_question_id
        WHERE item.practice_paper_id = :paperId
        """, nativeQuery = true)
    int sumPlannedSecondsForPaper(@Param("paperId") UUID paperId);

    @Query(value = """
        SELECT practice_question_id
        FROM practice_paper_items
        WHERE practice_paper_id = :paperId
        ORDER BY slot_order
        """, nativeQuery = true)
    List<UUID> findQuestionIdsForPaper(@Param("paperId") UUID paperId);

    /**
     * Deletes exactly the row at the HIGHEST slot of the paper, and only if it carries the specified question.
     *
     * <p>The constraint {@code practice_question_id = :questionId} is a safety latch, not redundant: between
     * the caller reading "what is the last question" and the delete, another {@code next-question} call may
     * have inserted a new slot. Without the constraint we would delete the question that was just chosen for the student.
     */
    @Modifying
    @Query(value = """
        DELETE FROM practice_paper_items
        WHERE practice_paper_id = :paperId
          AND practice_question_id = :questionId
          AND slot_order = (
              SELECT MAX(slot_order) FROM practice_paper_items WHERE practice_paper_id = :paperId
          )
        """, nativeQuery = true)
    int deleteLastItemForPaper(@Param("paperId") UUID paperId, @Param("questionId") UUID questionId);
}
