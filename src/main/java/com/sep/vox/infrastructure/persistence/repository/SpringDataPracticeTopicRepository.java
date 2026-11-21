package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.DimensionScoreInfo;
import com.sep.vox.application.query.dto.RankedTopicInfo;
import com.sep.vox.application.query.dto.TopicDimensionInfo;
import com.sep.vox.application.query.dto.TopicNameCardInfo;
import com.sep.vox.application.query.dto.TopicSearchRowInfo;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeTopicJpaEntity;

public interface SpringDataPracticeTopicRepository
        extends JpaRepository<PracticeTopicJpaEntity, UUID> {


    /**
     * The size of the topic pool that FEEDS the greeting batch -- used to decide whether to generate more or fewer each session.
     *
     * <p>{@code ASSESSMENT_QUESTION_BANK} must be subtracted: those are topics lazily materialized from the school's
     * question bank for ASSESSMENT_PREP students, and {@code findRankedTopics} already excludes them with
     * {@code source IS DISTINCT FROM 'ASSESSMENT_QUESTION_BANK'}. Counting them too would make a school with
     * 200 question-bank topics exceed the threshold immediately, and students practicing freely would be throttled from generating topics
     * because of a pool they never see.
     */
    long countByActiveTrueAndSourceNot(String source);

    // GỠ 2026-08-11: findByActiveTrueOrderByName() và findByActiveTrue(). Cả hai trả entity đầy đủ
    // (kèm cột description kiểu TEXT) cho những chỗ chỉ cần tên -- nay dùng findActiveNameCards()
    // bên dưới.

    Optional<PracticeTopicJpaEntity> findByNormalizedName(String normalizedName);

    boolean existsByIdAndActiveTrue(UUID id);

    /**
     * Chiều sở thích của đúng những chủ đề được hỏi tới -- thay cho {@code findAll()} thuần mà
     * {@code findAllTopicDimensions()} cũ dùng (gỡ 2026-08-11).
     *
     * <p>KHÔNG lọc {@code active}: bản cũ cũng không lọc, và sự kiện quan tâm nằm trên chủ đề đã
     * tắt vẫn phải được tính vào điểm chiều. Thêm lọc là đổi kết quả một cách âm thầm.
     */
    @Query(value = """
        SELECT topic.id AS id, topic.interest_dimensions AS interestDimension
        FROM practice_topics topic
        WHERE topic.id IN (:topicIds)
        """, nativeQuery = true)
    List<TopicDimensionInfo> findDimensionsByIds(@Param("topicIds") Collection<UUID> topicIds);

    /**
     * A name card of the active topics -- used for the duplicate check by name in
     * {@code TopicSuggestionService.findNearExistingTopic}.
     *
     * <p>Replaces {@code findByActiveTrue()}, which returned full entities. The comparison only touches {@code name},
     * and the result only reads {@code id}/{@code name}/{@code interestDimension}.
     *
     * <p>It does NOT filter {@code source}: topics materialized from the question bank ({@code ASSESSMENT_QUESTION_BANK})
     * must also be in the candidate set, exactly like the old {@code findByActiveTrue()}. They are NOT
     * in Chroma (the materialization path does not call {@code generationClient.index}), so this is the
     * only place that can block duplicates against them.
     */
    @Query(value = """
        SELECT topic.id AS id, topic.name AS name,
               topic.interest_dimensions AS interestDimension
        FROM practice_topics topic
        WHERE topic.active = true
        """, nativeQuery = true)
    List<TopicNameCardInfo> findActiveNameCards();

    @Query(value = """
        WITH profile AS (
            -- Không còn ORDER BY version: hồ sơ nay 1-1 với học sinh (unique index
            -- idx_learner_profile_student), nên chỉ có đúng một dòng để lấy. LIMIT 1 giữ lại
            -- làm chốt chặn cho LEFT JOIN bên dưới -- hai dòng ở đây sẽ nhân đôi mọi chủ đề.
            SELECT id
            FROM learner_profiles
            WHERE student_id = :studentId
            LIMIT 1
        )
        SELECT topic.id AS id,
               topic.name AS name,
               topic.interest_dimensions AS interestDimension,
               topic.curriculum_group AS curriculumGroup,
               COALESCE(topic_score.score, 0.5) AS topicScore,
               COALESCE(topic_score.sessions_mentioned, 0) AS mentions,
               COALESCE(dimension_score.score, 0.5) AS dimensionScore,
               CASE
                   WHEN topic_score.last_mentioned_at IS NULL THEN 0.0
                   ELSE EXP(
                       -EXTRACT(EPOCH FROM (
                           CURRENT_TIMESTAMP - topic_score.last_mentioned_at
                       )) / 86400.0 / 7.0
                   )
               END AS recency,
               EXISTS (
                   SELECT 1
                   FROM saved_topics saved
                   WHERE saved.student_id = :studentId
                     AND saved.practice_topic_id = topic.id
               ) AS savedByMe
        FROM practice_topics topic
        LEFT JOIN topic_interest_scores topic_score
          ON topic_score.student_id = :studentId
         AND topic_score.practice_topic_id = topic.id
        LEFT JOIN profile ON true
        LEFT JOIN dimension_interest_scores dimension_score
          ON dimension_score.learner_profile_id = profile.id
         AND dimension_score.dimension = topic.interest_dimensions
        WHERE topic.active = true
          AND topic.source IS DISTINCT FROM 'ASSESSMENT_QUESTION_BANK'
        """, nativeQuery = true)
    List<RankedTopicInfo> findRankedTopics(
        @Param("studentId") UUID studentId,
        @Param("goal") String goal
    );

    @Query(value = """
        SELECT topic.id AS id, topic.name AS name, topic.interest_dimensions AS interestDimension,
               true AS savedByMe
        FROM practice_topics topic
        JOIN saved_topics saved ON saved.practice_topic_id = topic.id
        WHERE saved.student_id = :studentId AND topic.active = true
        ORDER BY saved.saved_at DESC
        """, nativeQuery = true)
    List<TopicSearchRowInfo> findSavedTopics(@Param("studentId") UUID studentId);

    @Query(value = """
        SELECT topic.id AS id, topic.name AS name, topic.interest_dimensions AS interestDimension,
               EXISTS (
                   SELECT 1 FROM saved_topics saved
                   WHERE saved.student_id = :studentId
                     AND saved.practice_topic_id = topic.id
               ) AS savedByMe
        FROM practice_topics topic
        WHERE topic.active = true
          AND (
              LOWER(topic.name) LIKE :pattern
              OR topic.normalized_name LIKE :pattern
          )
        ORDER BY
            CASE WHEN LOWER(topic.name) = :normalized THEN 0 ELSE 1 END,
            topic.name
        LIMIT 10
        """, nativeQuery = true)
    List<TopicSearchRowInfo> searchTopics(
        @Param("studentId") UUID studentId,
        @Param("pattern") String pattern,
        @Param("normalized") String normalized
    );

    /**
     * Nạp lại chủ đề theo danh sách id -- dùng cho kết quả tìm bằng vector.
     *
     * Chroma chỉ trả id; tên và mô tả bên đó là bản chụp lúc index nên có thể đã cũ, và chủ đề
     * có thể đã bị tắt active sau đó. Đọc lại từ Postgres để hiển thị đúng hiện trạng, đồng thời
     * lọc luôn chủ đề không còn dùng được.
     */
    @Query(value = """
        SELECT topic.id AS id, topic.name AS name, topic.interest_dimensions AS interestDimension,
               EXISTS (
                   SELECT 1 FROM saved_topics saved
                   WHERE saved.student_id = :studentId
                     AND saved.practice_topic_id = topic.id
               ) AS savedByMe
        FROM practice_topics topic
        WHERE topic.active = true AND topic.id IN (:topicIds)
        """, nativeQuery = true)
    List<TopicSearchRowInfo> findActiveByIds(
        @Param("studentId") UUID studentId,
        @Param("topicIds") Collection<UUID> topicIds
    );

    @Query(value = """
        SELECT topic.id AS id, topic.name AS name, topic.interest_dimensions AS interestDimension,
               EXISTS (
                   SELECT 1 FROM saved_topics saved
                   WHERE saved.student_id = :studentId
                     AND saved.practice_topic_id = topic.id
               ) AS savedByMe
        FROM practice_topics topic
        WHERE topic.active = true
        ORDER BY RANDOM()
        LIMIT 1
        """, nativeQuery = true)
    Optional<TopicSearchRowInfo> findRandomActiveTopic(@Param("studentId") UUID studentId);

    @Query(value = """
        SELECT topic.name
        FROM practice_topics topic
        WHERE topic.active = true
          AND EXISTS (
              SELECT 1
              FROM student_question_exposures exposure
              JOIN practice_questions question
                ON question.id = exposure.practice_question_id
              WHERE exposure.student_id = :studentId
                AND question.practice_topic_id = topic.id
          )
          AND NOT EXISTS (
              SELECT 1
              FROM practice_questions question
              WHERE question.practice_topic_id = topic.id
                AND question.active = true
                AND NOT EXISTS (
                    SELECT 1
                    FROM student_question_exposures exposure
                    WHERE exposure.student_id = :studentId
                      AND exposure.practice_question_id = question.id
                )
          )
        ORDER BY topic.name
        """, nativeQuery = true)
    List<String> findExhaustedTopicNames(@Param("studentId") UUID studentId);

    Optional<PracticeTopicJpaEntity> findBySourceQuestionTopicId(UUID sourceQuestionTopicId);

    @Query(value = """
        SELECT qt.id AS id, qt.name AS name, qt.description AS description
        FROM question_topics qt
        JOIN question_banks qb ON qb.id = qt.question_bank_id
        WHERE qt.status = 'PUBLISHED'
          AND qb.status = 'PUBLISHED'
          AND qb.owner_type = 'SCHOOL'
          AND qb.school_id = :schoolId
          AND (
              NOT EXISTS (
                  SELECT 1 FROM question_bank_grades g WHERE g.question_bank_id = qb.id
              )
              OR EXISTS (
                  SELECT 1 FROM question_bank_grades g
                  WHERE g.question_bank_id = qb.id AND g.school_grade_id = :gradeId
              )
          )
        ORDER BY qt.name
        """, nativeQuery = true)
    List<com.sep.vox.application.query.dto.QuestionTopicInfo> findPublishedAssessmentTopics(
        @Param("schoolId") UUID schoolId,
        @Param("gradeId") UUID gradeId
    );

    @Query(value = """
        SELECT score.dimension AS dimension, score.score AS score
        FROM dimension_interest_scores score
        JOIN learner_profiles profile
          ON profile.id = score.learner_profile_id
        WHERE profile.id = (
            SELECT id
            FROM learner_profiles
            WHERE student_id = :studentId
            LIMIT 1
        )
        """, nativeQuery = true)
    List<DimensionScoreInfo> findInterestScores(@Param("studentId") UUID studentId);
}
