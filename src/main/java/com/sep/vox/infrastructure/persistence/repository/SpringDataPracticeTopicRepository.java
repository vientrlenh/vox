package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.DimensionScoreInfo;
import com.sep.vox.application.query.dto.RankedTopicInfo;
import com.sep.vox.application.query.dto.TopicSearchRowInfo;
import com.sep.vox.infrastructure.persistence.entity.PracticeTopicJpaEntity;

public interface SpringDataPracticeTopicRepository
        extends JpaRepository<PracticeTopicJpaEntity, UUID> {

    List<PracticeTopicJpaEntity> findByActiveTrue();

    /**
     * Cỡ kho chủ đề NUÔI LÔ CHÀO -- dùng để quyết định sinh thêm nhiều hay ít mỗi phiên.
     *
     * <p>Phải trừ {@code EXAM_QUESTION_BANK}: đó là chủ đề vật chất hoá lazy từ ngân hàng đề
     * của trường cho học sinh EXAM_PREP, và {@code findRankedTopics} đã loại chúng bằng
     * {@code source IS DISTINCT FROM 'EXAM_QUESTION_BANK'}. Đếm cả vào thì một trường có
     * 200 chủ đề ngân hàng đề sẽ vượt ngưỡng ngay, và học sinh luyện tự do bị siết sinh chủ đề
     * vì một kho họ không bao giờ thấy.
     */
    long countByActiveTrueAndSourceNot(String source);

    List<PracticeTopicJpaEntity> findByActiveTrueOrderByName();

    Optional<PracticeTopicJpaEntity> findByNormalizedName(String normalizedName);

    boolean existsByIdAndActiveTrue(UUID id);

    @Query(value = """
        WITH profile AS (
            SELECT id
            FROM learner_profile
            WHERE student_id = :studentId
            ORDER BY version DESC
            LIMIT 1
        )
        SELECT topic.id AS id,
               topic.name AS name,
               topic.interest_dimension AS interestDimension,
               topic.curriculum_group AS curriculumGroup,
               COALESCE(topic_score.score, 0.5) AS topicScore,
               COALESCE(topic_score.sessions_mentioned, 0) AS mentions,
               COALESCE(dimension_score.score, 0.5) AS dimensionScore,
               COUNT(question.id) FILTER (
                   WHERE question.active = true
                     AND exposure.id IS NULL
               )::int AS unseenCount,
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
                   FROM saved_topic saved
                   WHERE saved.student_id = :studentId
                     AND saved.practice_topic_id = topic.id
               ) AS savedByMe
        FROM practice_topic topic
        LEFT JOIN topic_interest_score topic_score
          ON topic_score.student_id = :studentId
         AND topic_score.practice_topic_id = topic.id
        LEFT JOIN profile ON true
        LEFT JOIN dimension_interest_score dimension_score
          ON dimension_score.learner_profile_id = profile.id
         AND dimension_score.dimension = topic.interest_dimension
        LEFT JOIN practice_question question
          ON question.practice_topic_id = topic.id
        LEFT JOIN student_question_exposure exposure
          ON exposure.student_id = :studentId
         AND exposure.practice_question_id = question.id
        WHERE topic.active = true
          AND topic.source IS DISTINCT FROM 'EXAM_QUESTION_BANK'
        GROUP BY topic.id, topic.name, topic.interest_dimension,
                 topic.curriculum_group, topic_score.score,
                 topic_score.sessions_mentioned,
                 topic_score.last_mentioned_at, dimension_score.score
        """, nativeQuery = true)
    List<RankedTopicInfo> findRankedTopics(
        @Param("studentId") UUID studentId,
        @Param("goal") String goal
    );

    @Query(value = """
        SELECT topic.id AS id, topic.name AS name, topic.interest_dimension AS interestDimension,
               true AS savedByMe
        FROM practice_topic topic
        JOIN saved_topic saved ON saved.practice_topic_id = topic.id
        WHERE saved.student_id = :studentId AND topic.active = true
        ORDER BY saved.saved_at DESC
        """, nativeQuery = true)
    List<TopicSearchRowInfo> findSavedTopics(@Param("studentId") UUID studentId);

    @Query(value = """
        SELECT topic.id AS id, topic.name AS name, topic.interest_dimension AS interestDimension,
               EXISTS (
                   SELECT 1 FROM saved_topic saved
                   WHERE saved.student_id = :studentId
                     AND saved.practice_topic_id = topic.id
               ) AS savedByMe
        FROM practice_topic topic
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

    @Query(value = """
        SELECT topic.id AS id, topic.name AS name, topic.interest_dimension AS interestDimension,
               EXISTS (
                   SELECT 1 FROM saved_topic saved
                   WHERE saved.student_id = :studentId
                     AND saved.practice_topic_id = topic.id
               ) AS savedByMe
        FROM practice_topic topic
        WHERE topic.active = true
        ORDER BY RANDOM()
        LIMIT 1
        """, nativeQuery = true)
    Optional<TopicSearchRowInfo> findRandomActiveTopic(@Param("studentId") UUID studentId);

    @Query(value = """
        SELECT topic.name
        FROM practice_topic topic
        WHERE topic.active = true
          AND EXISTS (
              SELECT 1
              FROM student_question_exposure exposure
              JOIN practice_question question
                ON question.id = exposure.practice_question_id
              WHERE exposure.student_id = :studentId
                AND question.practice_topic_id = topic.id
          )
          AND NOT EXISTS (
              SELECT 1
              FROM practice_question question
              WHERE question.practice_topic_id = topic.id
                AND question.active = true
                AND NOT EXISTS (
                    SELECT 1
                    FROM student_question_exposure exposure
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
    List<com.sep.vox.application.query.dto.QuestionTopicInfo> findPublishedExamTopics(
        @Param("schoolId") UUID schoolId,
        @Param("gradeId") UUID gradeId
    );

    @Query(value = """
        SELECT score.dimension AS dimension, score.score AS score
        FROM dimension_interest_score score
        JOIN learner_profile profile
          ON profile.id = score.learner_profile_id
        WHERE profile.id = (
            SELECT id
            FROM learner_profile
            WHERE student_id = :studentId
            ORDER BY version DESC
            LIMIT 1
        )
        """, nativeQuery = true)
    List<DimensionScoreInfo> findInterestScores(@Param("studentId") UUID studentId);
}
