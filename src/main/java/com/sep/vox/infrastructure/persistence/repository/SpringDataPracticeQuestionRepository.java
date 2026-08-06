package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.QuestionWithTopicInfo;
import com.sep.vox.infrastructure.persistence.entity.PracticeQuestionJpaEntity;

public interface SpringDataPracticeQuestionRepository
        extends JpaRepository<PracticeQuestionJpaEntity, UUID> {

    @Query(value = """
        SELECT question.question_text AS questionText,
               question.evaluation_guide_json AS evaluationGuideJson,
               question.question_type AS questionType,
               question.min_response_seconds AS minResponseSeconds,
               question.max_response_seconds AS maxResponseSeconds,
               topic.name AS topicName,
               topic.description AS topicDescription
        FROM practice_question question
        JOIN practice_topic topic
          ON topic.id = question.practice_topic_id
        WHERE question.id = :questionId
        """, nativeQuery = true)
    Optional<QuestionWithTopicInfo> findQuestionWithTopic(@Param("questionId") UUID questionId);

    // Không có caller sống (đề bài đã đổi sang findUnseenByTopicAndCriterionAndRankRange/
    // findUnseenByIds cho ladder chọn câu, xem mục 3 gói 11) -- không áp band/cooldown ở đây
    // vì không có gì gọi tới, tránh SQL chết không ai kiểm chứng.
    @Query(value = """
        SELECT question.*
        FROM practice_question question
        WHERE question.practice_topic_id = :topicId
          AND question.active = true
          AND NOT EXISTS (
              SELECT 1
              FROM student_question_exposure exposure
              WHERE exposure.student_id = :studentId
                AND exposure.practice_question_id = question.id
          )
        ORDER BY question.usage_count, question.created_at
        """, nativeQuery = true)
    List<PracticeQuestionJpaEntity> findUnseenByTopic(
        @Param("topicId") UUID topicId,
        @Param("studentId") UUID studentId
    );

    @Query(value = """
        SELECT question.*
        FROM practice_question question
        WHERE question.practice_topic_id = :topicId
          AND question.active = true
          AND (:criterion IS NULL OR question.target_criterion_code = :criterion)
          AND question.difficulty_rank BETWEEN :rankMin AND :rankMax
          AND NOT EXISTS (
              SELECT 1
              FROM student_question_exposure exposure
              -- So sánh theo result_band_order (SỐ), không nối chuỗi 'BAC_' || rank rồi so
              -- sánh chuỗi như trước. Cách cũ dính cứng cách đặt mã của VSTEP: đổi sang CEFR
              -- (A1..C2) là so 'BAC_3' với 'B1' -- vô nghĩa mà KHÔNG nổ, chỉ lặng lẽ trả sai
              -- tập câu hỏi. Và ngay với VSTEP nó cũng sai sẵn nếu có ≥10 bậc, vì theo thứ tự
              -- chuỗi thì 'BAC_10' < 'BAC_9'.
              LEFT JOIN LATERAL (
                  SELECT MAX(band.result_band_order) AS best_band_order
                  FROM practice_item_response response
                  JOIN practice_item_evaluation evaluation
                    ON evaluation.practice_response_id = response.id
                   AND evaluation.marked_invalid = false
                  JOIN practice_criterion_score score
                    ON score.practice_evaluation_id = evaluation.id
                  JOIN rubric_criterions rc
                    ON rc.id = score.rubric_criterion_id
                   AND UPPER(rc.code) = UPPER(question.target_criterion_code)
                  JOIN framework_criteria fc
                    ON fc.id = rc.framework_criterion_id
                  JOIN framework_result_bands band
                    ON band.framework_version_id = fc.framework_version_id
                   AND band.code = score.matched_band_code
                  WHERE response.practice_question_id = exposure.practice_question_id
                    AND response.practice_session_id IN (
                        SELECT id FROM practice_session WHERE student_id = exposure.student_id
                    )
              ) target_score ON true
              WHERE exposure.student_id = :studentId
                AND exposure.practice_question_id = question.id
                AND (
                      target_score.best_band_order IS NULL
                   OR target_score.best_band_order >= question.difficulty_rank
                   OR exposure.seen_at > :cooldownCutoff
                )
          )
        ORDER BY question.usage_count, question.created_at
        """, nativeQuery = true)
    List<PracticeQuestionJpaEntity> findUnseenByTopicAndCriterionAndRankRange(
        @Param("topicId") UUID topicId,
        @Param("studentId") UUID studentId,
        @Param("criterion") String criterion,
        @Param("rankMin") int rankMin,
        @Param("rankMax") int rankMax,
        @Param("cooldownCutoff") Instant cooldownCutoff
    );

    @Query(value = """
        SELECT question.*
        FROM practice_question question
        WHERE question.id IN :questionIds
          AND question.active = true
          AND NOT EXISTS (
              SELECT 1
              FROM student_question_exposure exposure
              -- So sánh theo result_band_order (SỐ), không nối chuỗi 'BAC_' || rank rồi so
              -- sánh chuỗi như trước. Cách cũ dính cứng cách đặt mã của VSTEP: đổi sang CEFR
              -- (A1..C2) là so 'BAC_3' với 'B1' -- vô nghĩa mà KHÔNG nổ, chỉ lặng lẽ trả sai
              -- tập câu hỏi. Và ngay với VSTEP nó cũng sai sẵn nếu có ≥10 bậc, vì theo thứ tự
              -- chuỗi thì 'BAC_10' < 'BAC_9'.
              LEFT JOIN LATERAL (
                  SELECT MAX(band.result_band_order) AS best_band_order
                  FROM practice_item_response response
                  JOIN practice_item_evaluation evaluation
                    ON evaluation.practice_response_id = response.id
                   AND evaluation.marked_invalid = false
                  JOIN practice_criterion_score score
                    ON score.practice_evaluation_id = evaluation.id
                  JOIN rubric_criterions rc
                    ON rc.id = score.rubric_criterion_id
                   AND UPPER(rc.code) = UPPER(question.target_criterion_code)
                  JOIN framework_criteria fc
                    ON fc.id = rc.framework_criterion_id
                  JOIN framework_result_bands band
                    ON band.framework_version_id = fc.framework_version_id
                   AND band.code = score.matched_band_code
                  WHERE response.practice_question_id = exposure.practice_question_id
                    AND response.practice_session_id IN (
                        SELECT id FROM practice_session WHERE student_id = exposure.student_id
                    )
              ) target_score ON true
              WHERE exposure.student_id = :studentId
                AND exposure.practice_question_id = question.id
                AND (
                      target_score.best_band_order IS NULL
                   OR target_score.best_band_order >= question.difficulty_rank
                   OR exposure.seen_at > :cooldownCutoff
                )
          )
        ORDER BY question.usage_count, question.created_at
        """, nativeQuery = true)
    List<PracticeQuestionJpaEntity> findUnseenByIds(
        @Param("questionIds") List<UUID> questionIds,
        @Param("studentId") UUID studentId,
        @Param("cooldownCutoff") Instant cooldownCutoff
    );

    /**
     * Id các câu của MỘT chủ đề đã chết VĨNH VIỄN với học sinh này -- gửi xuống Python để loại
     * khỏi phép so trùng lúc sinh câu mới.
     *
     * <p>Vì sao cần: cổng chặn trùng bên Python (CandidateFilterNode, cosine >= 0,92) tra Chroma
     * với {@code where={"active": True}} -- nó KHÔNG có khái niệm học sinh nào và không thể có.
     * Còn luật loại câu thì lại theo từng học sinh. Hai phạm vi lệch nhau tạo ra khoá cứng:
     *
     * <pre>
     *   HS luyện câu A, đạt bậc  -> A loại vĩnh viễn với em ấy
     *   chọn lại chủ đề đó       -> bậc 1,2 rỗng
     *   nhờ LLM soạn câu mới     -> cùng chủ đề/tiêu chí/bậc nên rất giống A
     *   chặn trùng               -> cosine >= 0,92 với A -> vứt sạch
     *   -> pool_exhausted, và MÃI MÃI như vậy: thứ chặn nó là thứ nó không được phép dùng
     * </pre>
     *
     * <p>Chỉ lấy câu VĨNH VIỄN chết, không lấy câu đang trong 24h nghỉ: câu nghỉ rồi sẽ quay
     * lại, sinh thêm bản gần trùng với nó là tự tạo lặp cho chính em ấy vài ngày sau.
     *
     * <p>Bó theo chủ đề chứ không lấy toàn hệ: câu về âm nhạc gần như không thể đạt 0,92 với
     * câu về tình bạn, nên gửi cả kho chỉ làm phình payload.
     */
    @Query(value = """
        SELECT question.id
        FROM practice_question question
        JOIN student_question_exposure exposure
          ON exposure.practice_question_id = question.id
         AND exposure.student_id = :studentId
        LEFT JOIN LATERAL (
            SELECT MAX(band.result_band_order) AS best_band_order
            FROM practice_item_response response
            JOIN practice_item_evaluation evaluation
              ON evaluation.practice_response_id = response.id
             AND evaluation.marked_invalid = false
            JOIN practice_criterion_score score
              ON score.practice_evaluation_id = evaluation.id
            JOIN rubric_criterions rc
              ON rc.id = score.rubric_criterion_id
             AND UPPER(rc.code) = UPPER(question.target_criterion_code)
            JOIN framework_criteria fc
              ON fc.id = rc.framework_criterion_id
            JOIN framework_result_bands band
              ON band.framework_version_id = fc.framework_version_id
             AND band.code = score.matched_band_code
            WHERE response.practice_question_id = question.id
              AND response.practice_session_id IN (
                  SELECT id FROM practice_session WHERE student_id = :studentId
              )
        ) target_score ON true
        WHERE question.practice_topic_id = :topicId
          AND question.active = true
          AND (
                target_score.best_band_order IS NULL
             OR target_score.best_band_order >= question.difficulty_rank
          )
        """, nativeQuery = true)
    List<UUID> findPermanentlyExhaustedIds(
        @Param("topicId") UUID topicId,
        @Param("studentId") UUID studentId
    );

    @Modifying
    @Query(value = "UPDATE practice_question SET usage_count = usage_count + 1 WHERE id = :id", nativeQuery = true)
    void incrementUsageCount(@Param("id") UUID id);

    /**
     * Trả lại lượt dùng cho câu được CHỌN nhưng học sinh chưa bao giờ trả lời.
     *
     * <p>{@code GREATEST(0, ...)} chứ không trừ thẳng: cột này không âm được, và một lần trừ
     * hụt (dữ liệu cũ, sửa tay) không được phép làm hỏng bản ghi.
     */
    @Modifying
    @Query(
        value = "UPDATE practice_question SET usage_count = GREATEST(0, usage_count - 1) WHERE id = :id",
        nativeQuery = true
    )
    void decrementUsageCount(@Param("id") UUID id);

    /**
     * Câu do generationClient sinh mang ID được gán sẵn từ phía client (dùng làm khoá đồng bộ
     * với Chroma) -- không thể đi qua {@code save()} vì entity ánh xạ {@code id} là
     * DB-generated (insertable=false). Giữ nguyên INSERT ... ON CONFLICT DO NOTHING như bản gốc.
     */
    @Modifying
    @Query(value = """
        INSERT INTO practice_question (
            id, practice_topic_id, question_text,
            target_criterion_code, target_sub_attribute,
            difficulty_rank, difficulty_features_json,
            evaluation_guide_json, suggested_ideas_json,
            question_type,
            max_response_seconds, min_response_seconds,
            vstep_part, source,
            usage_count, active, created_at
        ) VALUES (:id, :topicId, :questionText, :criterionCode, :subAttribute,
                  :difficultyRank, :difficultyFeaturesJson, :evaluationGuideJson,
                  :suggestedIdeasJson, :questionType,
                  :maxResponseSeconds, :minResponseSeconds,
                  :vstepPart, 'AI_GENERATED', 0, true, CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO NOTHING
        """, nativeQuery = true)
    void insertGeneratedQuestion(
        @Param("id") UUID id,
        @Param("topicId") UUID topicId,
        @Param("questionText") String questionText,
        @Param("criterionCode") String criterionCode,
        @Param("subAttribute") String subAttribute,
        @Param("difficultyRank") int difficultyRank,
        @Param("difficultyFeaturesJson") String difficultyFeaturesJson,
        @Param("evaluationGuideJson") String evaluationGuideJson,
        @Param("suggestedIdeasJson") String suggestedIdeasJson,
        @Param("questionType") String questionType,
        @Param("maxResponseSeconds") int maxResponseSeconds,
        @Param("minResponseSeconds") int minResponseSeconds,
        @Param("vstepPart") Integer vstepPart
    );
}
