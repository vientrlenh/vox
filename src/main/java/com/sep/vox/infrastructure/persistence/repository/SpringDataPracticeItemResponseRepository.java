package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    /**
     * Diện cần XẢ CHẤM. Ba nhánh, mỗi nhánh chặn một kiểu hỏng khác nhau:
     *
     * <ul>
     *   <li>{@code PENDING} -- chưa từng gửi. Diện mồ côi thật.</li>
     *   <li>{@code GRADING} quá hạn -- đã gửi mà im quá lâu. Bắt đúng những ca KHÔNG AI BÁO
     *       ĐƯỢC: pod agents bị OOM-kill, node bị evict, mất Kafka lúc publish. Không có nhánh
     *       này thì dòng nằm GRADING vĩnh viễn và không bao giờ được cứu.</li>
     *   <li>{@code GRADING_FAILED} còn lượt -- agents báo hỏng, thử lại nhưng CÓ TRẦN. Hỏng do
     *       dữ liệu (audio vỡ, transcript rỗng) thì thử bao nhiêu lần cũng hỏng y hệt; bắn lại
     *       vô hạn chỉ đốt tiền LLM và giữ màn tổng kết quay mãi.</li>
     * </ul>
     *
     * <p>{@code GRADED} không bao giờ lọt vào -- đó là điều kiện dừng.
     *
     * <p>Trước khi có ba nhánh này, luật cũ chỉ hỏi "đã có bản chấm chưa", mà chấm một câu mất
     * ~3,5 phút -- lâu hơn nhịp quét 5 phút. Nên mọi câu đều bị bắn lại trước khi kịp chấm xong,
     * bản sao lại chiếm thêm 3,5 phút của consumer, các câu sau càng chậm nên càng bị bắn lại.
     * Đo thật 2026-08-12: một câu bị chấm lại 8 LẦN, hàng đợi tồn 15 message và vẫn lớn dần
     * trong khi offset consumer đứng yên.
     */
    @Query(value = """
        SELECT response.id AS responseId, response.practice_question_id AS questionId
        FROM practice_item_response response
        WHERE response.practice_session_id = :sessionId
          AND response.question_complete = false
          AND (
              response.grading_status = 'PENDING'
              OR (response.grading_status = 'GRADING'
                  AND (response.grading_requested_at IS NULL
                       OR response.grading_requested_at < :requestedBefore))
              OR (response.grading_status = 'GRADING_FAILED'
                  AND response.grading_attempts < :maxAttempts)
          )
          AND NOT EXISTS (
              SELECT 1
              FROM practice_item_evaluation evaluation
              WHERE evaluation.practice_response_id = response.id
          )
        """, nativeQuery = true)
    List<PendingEvaluationResponse> findResponsesAwaitingFlush(
        @Param("sessionId") UUID sessionId,
        @Param("requestedBefore") Instant requestedBefore,
        @Param("maxAttempts") int maxAttempts
    );

    /**
     * Đóng dấu ĐÃ GỬI yêu cầu chấm. Gọi NGAY SAU khi publish thành công, không phải trước --
     * đánh dấu trước rồi publish hỏng thì câu đó bị khoá ngoài diện quét suốt cả cửa sổ nguội.
     *
     * <p>{@code grading_attempts} cộng dồn ở ĐÂY chứ không ở chỗ báo hỏng: ta cần đếm số lần ĐÃ
     * THỬ, mà một lần thử có thể chết lặng không bao giờ báo hỏng. Đếm ở nhánh hỏng thì đúng
     * những ca im lặng ấy lại được thử vô hạn.
     */
    @Modifying
    @Query(value = """
        UPDATE practice_item_response
        SET grading_status = 'GRADING',
            grading_requested_at = :requestedAt,
            grading_attempts = grading_attempts + 1
        WHERE id = :responseId
        """, nativeQuery = true)
    void markGradingRequested(
        @Param("responseId") UUID responseId,
        @Param("requestedAt") Instant requestedAt
    );

    /** Bản chấm đã về. Gọi CÙNG TRANSACTION với lúc ghi practice_item_evaluation. */
    @Modifying
    @Query(value = "UPDATE practice_item_response SET grading_status = 'GRADED' WHERE id = :responseId",
           nativeQuery = true)
    void markGraded(@Param("responseId") UUID responseId);

    /**
     * Số câu ĐÃ BỎ CUỘC: hỏng và hết lượt thử. Màn tổng kết dùng con số này để nói thẳng "chấm
     * không xong" thay vì quay vòng chờ một kết quả sẽ không bao giờ tới.
     */
    @Query(value = """
        SELECT COUNT(*)::int
        FROM practice_item_response response
        WHERE response.practice_session_id = :sessionId
          AND response.grading_status = 'GRADING_FAILED'
          AND response.grading_attempts >= :maxAttempts
        """, nativeQuery = true)
    int countGradingGaveUp(@Param("sessionId") UUID sessionId, @Param("maxAttempts") int maxAttempts);

    /** Agents báo chấm hỏng. Số lần thử đã cộng lúc gửi nên ở đây chỉ đổi trạng thái. */
    @Modifying
    @Query(value = "UPDATE practice_item_response SET grading_status = 'GRADING_FAILED' WHERE id = :responseId",
           nativeQuery = true)
    void markGradingFailed(@Param("responseId") UUID responseId);

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
