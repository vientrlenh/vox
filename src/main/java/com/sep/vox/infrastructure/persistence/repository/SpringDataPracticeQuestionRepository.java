package com.sep.vox.infrastructure.persistence.repository;

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

    @Query(value = """
        SELECT question.*
        FROM practice_question question
        WHERE question.practice_topic_id = :topicId
          AND question.active = true
          AND (:criterion IS NULL OR question.target_criterion_code = :criterion)

          AND (
                :tense IS NULL
             OR question.target_tense IS NULL
             OR question.target_tense = :tense
          )
          AND question.difficulty_rank BETWEEN :rankMin AND :rankMax
          -- ĐÃ GẶP LÀ KHÔNG BAO GIỜ GẶP LẠI (2026-08-06). Chỉ một điều kiện: có dòng exposure
          -- là loại. Bản cũ cho câu quay lại nếu học sinh được chấm DƯỚI bậc của câu và đã qua
          -- thời gian chờ -- kèm theo là một LATERAL join qua 5 bảng (response → evaluation →
          -- criterion_score → rubric_criterions → framework_result_bands) chỉ để biết điều đó.
          -- Bỏ cả luật lẫn join.
          AND NOT EXISTS (
              SELECT 1
              FROM student_question_exposure exposure
              WHERE exposure.student_id = :studentId
                AND exposure.practice_question_id = question.id
          )
        ORDER BY question.usage_count, question.created_at
        """, nativeQuery = true)
    List<PracticeQuestionJpaEntity> findUnseenByTopicAndCriterionAndRankRange(
        @Param("topicId") UUID topicId,
        @Param("studentId") UUID studentId,
        @Param("criterion") String criterion,
        @Param("tense") String tense,
        @Param("rankMin") int rankMin,
        @Param("rankMax") int rankMax
    );

    // "Cạn vĩnh viễn" = đã gặp. Từ 2026-08-06 không còn khái niệm gặp-lại-nếu-làm-chưa-đạt,
    // nên mọi câu đã gặp đều cạn -- điều kiện trùng khít với bộ lọc ở
    // findUnseenByTopicAndCriterionAndRankRange, và hai chỗ PHẢI trùng nhau: lệch một chút là
    // chủ đề bị coi là còn câu trong khi thang leo không tìm ra câu nào, hoặc ngược lại.
    @Query(value = """
        SELECT question.id
        FROM practice_question question
        JOIN student_question_exposure exposure
          ON exposure.practice_question_id = question.id
         AND exposure.student_id = :studentId
        WHERE question.practice_topic_id = :topicId
          AND question.active = true
        """, nativeQuery = true)
    List<UUID> findPermanentlyExhaustedIds(
        @Param("topicId") UUID topicId,
        @Param("studentId") UUID studentId
    );

    @Modifying
    @Query(value = "UPDATE practice_question SET usage_count = usage_count + 1 WHERE id = :id", nativeQuery = true)
    void incrementUsageCount(@Param("id") UUID id);

    
    @Modifying
    @Query(
        value = "UPDATE practice_question SET usage_count = GREATEST(0, usage_count - 1) WHERE id = :id",
        nativeQuery = true
    )
    void decrementUsageCount(@Param("id") UUID id);

 
    /**
     * ⚠️ Cột target_tense TỪNG BỊ SÓT ở đây (sửa 2026-08-07).
     *
     * Thì đích đi hết cả chuỗi -- TensePolicy.forSlot -> generateAndStore -> payload gửi Python
     * -> Python trả về -> PracticeQuestion giữ trong object -- rồi bị vứt ở đúng bước ghi cuối
     * vì câu INSERT này không liệt kê cột. Không lỗi, không cảnh báo: mọi câu sinh ra đều có
     * target_tense NULL, nên bộ lọc thì ở findUnseenByTopicAndCriterionAndRankRange
     * (`question.target_tense IS NULL OR = :tense`) luôn rơi vào vế đầu và cả cơ chế ép thì
     * chưa từng chạy.
     *
     * Thêm cột mới vào practice_question thì PHẢI sửa cả đây -- đây là đường ghi duy nhất của
     * câu do AI sinh, và nó không dùng entity nên Hibernate không nhắc.
     */
    @Modifying
    @Query(value = """
        INSERT INTO practice_question (
            id, practice_topic_id, question_text,
            target_criterion_code, target_sub_attribute, target_tense,
            difficulty_rank, difficulty_features_json,
            evaluation_guide_json, suggested_ideas_json,
            question_type,
            max_response_seconds, min_response_seconds,
            vstep_part, source,
            usage_count, active, created_at
        ) VALUES (:id, :topicId, :questionText, :criterionCode, :subAttribute, :targetTense,
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
        @Param("targetTense") String targetTense,
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
