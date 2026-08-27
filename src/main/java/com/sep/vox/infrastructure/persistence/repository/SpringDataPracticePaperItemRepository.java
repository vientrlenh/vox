package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.PracticePaperItemJpaEntity;

public interface SpringDataPracticePaperItemRepository
        extends JpaRepository<PracticePaperItemJpaEntity, UUID> {

    @Query(value = """
        -- Ngân sách DỰ TRÙ của đề = tổng TRẦN nói của các câu. Không cộng min_response_seconds
        -- (đó là sàn để biết khi nào trả lời đã đủ, không phải một khoản chi thêm); hai cột
        -- max_followup_seconds và preparation_time_seconds cũ đã bỏ -- xem V11.
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
     * Xoá đúng dòng ở slot CAO NHẤT của paper, và chỉ khi nó mang đúng câu được chỉ định.
     *
     * <p>Ràng buộc {@code practice_question_id = :questionId} là chốt an toàn, không thừa: giữa
     * lúc caller đọc "câu cuối là gì" và lúc xoá, một lượt {@code next-question} khác có thể đã
     * chèn thêm slot mới. Không ràng thì ta xoá nhầm câu vừa được chọn cho học sinh.
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
