package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.domain.repository.PendingEvaluationResponse;
import com.sep.vox.infrastructure.persistence.entity.PracticeItemResponseJpaEntity;

public interface SpringDataPracticeItemResponseRepository
        extends JpaRepository<PracticeItemResponseJpaEntity, UUID> {

    Optional<PracticeItemResponseJpaEntity> findByPracticeSessionIdAndPracticeQuestionId(
        UUID practiceSessionId,
        UUID practiceQuestionId
    );

    @Query(value = """
        SELECT session.rubric_version_id
        FROM practice_item_response response
        JOIN practice_session session ON session.id = response.practice_session_id
        WHERE response.id = :responseId
        """, nativeQuery = true)
    UUID findRubricVersionIdByResponseId(@Param("responseId") UUID responseId);

    @Query(value = "SELECT practice_session_id FROM practice_item_response WHERE id = :responseId",
           nativeQuery = true)
    UUID findSessionIdByResponseId(@Param("responseId") UUID responseId);

    // Do kho THUC TE cua buoi: trung binh difficulty_rank cua cac cau hoc sinh da tra loi.
    // Diem phien mot minh khong doc duoc -- no neo vao bac muc tieu, con do kho cau lai bam
    // theo bac hien tai cua hoc sinh. "6.5 o bac 3" va "6.5 o bac 4" la hai chuyen khac han.
    @Query(value = """
        SELECT AVG(question.difficulty_rank)
        FROM practice_item_response response
        JOIN practice_question question ON question.id = response.practice_question_id
        WHERE response.practice_session_id = :sessionId
        """, nativeQuery = true)
    Double findAverageDifficultyRank(@Param("sessionId") UUID sessionId);

    // "Chưa chấm" = có response (tức đã có người nói) mà chưa có practice_item_evaluation.
    // Chấm là bất đồng bộ nên con số này tự tụt về 0 khi kết quả lần lượt về -- màn tổng kết
    // poll đúng con số này để biết khi nào thôi chờ.
    @Query(value = """
        SELECT COUNT(*)::int
        FROM practice_item_response response
        WHERE response.practice_session_id = :sessionId
          AND NOT EXISTS (
              SELECT 1
              FROM practice_item_evaluation evaluation
              WHERE evaluation.practice_response_id = response.id
          )
        """, nativeQuery = true)
    int countAwaitingEvaluation(@Param("sessionId") UUID sessionId);

    // Diện cần XẢ CHẤM lúc đóng phiên: học sinh đã nói nhưng chuỗi follow-up chưa kết thúc,
    // nên SubmitPracticeTurnUseCase chưa từng bắn sự kiện chấm cho câu này. Không xả thì công
    // sức đó mất trắng -- quota đã trừ, lượt đã ghi, mà không có điểm lẫn quan sát điểm yếu.
    @Query(value = """
        SELECT response.id AS responseId, response.practice_question_id AS questionId
        FROM practice_item_response response
        WHERE response.practice_session_id = :sessionId
          AND response.question_complete = false
          AND NOT EXISTS (
              SELECT 1
              FROM practice_item_evaluation evaluation
              WHERE evaluation.practice_response_id = response.id
          )
        """, nativeQuery = true)
    List<PendingEvaluationResponse> findResponsesAwaitingFlush(@Param("sessionId") UUID sessionId);

    // Phiên ĐÃ ĐÓNG mà vẫn còn lượt chưa chấm -- những lượt mồ côi.
    //
    // Xả chấm chỉ chạy đúng lúc đóng phiên, nên hai nhóm lọt lưới: phiên đóng TRƯỚC khi có cơ
    // chế đó, và phiên mà lần bắn sự kiện chấm bị hỏng (Kafka nghẽn, Python chết). Cả hai
    // nhóm nằm đó vĩnh viễn, và màn tổng kết thì báo "đang chấm 1 câu" mãi không dứt -- gộp
    // "sắp có" với "không bao giờ có" làm một.
    //
    // Chặn theo :since (vd 24h) chứ không quét toàn bộ lịch sử: nếu việc chấm hỏng thật thì
    // quét không giới hạn sẽ bắn lại cùng bộ sự kiện mỗi 5 phút, mãi mãi. Quá hạn thì thôi,
    // và màn tổng kết nói thẳng là chấm không xong thay vì quay tiếp.
    @Query(value = """
        SELECT DISTINCT response.practice_session_id
        FROM practice_item_response response
        JOIN practice_session session ON session.id = response.practice_session_id
        WHERE session.ended_at IS NOT NULL
          AND session.ended_at >= :since
          AND NOT EXISTS (
              SELECT 1
              FROM practice_item_evaluation evaluation
              WHERE evaluation.practice_response_id = response.id
          )
        """, nativeQuery = true)
    List<UUID> findEndedSessionsWithUngradedResponses(@Param("since") Instant since);
}
