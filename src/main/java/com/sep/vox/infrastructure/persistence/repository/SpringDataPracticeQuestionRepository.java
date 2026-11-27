package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.QuestionWithTopicInfo;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeQuestionJpaEntity;

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
        FROM practice_questions question
        JOIN practice_topics topic
          ON topic.id = question.practice_topic_id
        WHERE question.id = :questionId
        """, nativeQuery = true)
    Optional<QuestionWithTopicInfo> findQuestionWithTopic(@Param("questionId") UUID questionId);

    @Query(value = """
        SELECT question.*
        FROM practice_questions question
        WHERE question.practice_topic_id = :topicId
          AND question.active = true
          AND (:criterion IS NULL OR question.target_criterion_code = :criterion)

          AND (
                :tense IS NULL
             OR question.target_tense IS NULL
             OR question.target_tense = :tense
          )
          AND question.difficulty_rank BETWEEN :rankMin AND :rankMax
          -- SEEN MEANS NEVER SEEN AGAIN (2026-08-06). A single condition: a row in exposure
          -- excludes the question. The old version let a question come back if the student was graded BELOW the band of the question and the
          -- waiting time had passed -- which came with a LATERAL join across 5 tables (response → evaluation →
          -- criterion_score → rubric_criterions → framework_result_bands) just to know that.
          -- Both the rule and the join are gone.
          AND NOT EXISTS (
              SELECT 1
              FROM student_question_exposures exposure
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

    // "Permanently exhausted" = seen. Since 2026-08-06 there is no seen-again-if-not-passed concept,
    // so every seen question is exhausted -- the condition is identical to the filter in
    // findUnseenByTopicAndCriterionAndRankRange, and the two places MUST match: a slight mismatch means
    // a topic is considered to still have questions while the climbing ladder finds none, or the opposite.
    @Query(value = """
        SELECT question.id
        FROM practice_questions question
        JOIN student_question_exposures exposure
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
    @Query(value = "UPDATE practice_questions SET usage_count = usage_count + 1 WHERE id = :id", nativeQuery = true)
    void incrementUsageCount(@Param("id") UUID id);

    
    @Modifying
    @Query(
        value = "UPDATE practice_questions SET usage_count = GREATEST(0, usage_count - 1) WHERE id = :id",
        nativeQuery = true
    )
    void decrementUsageCount(@Param("id") UUID id);

 
    /**
     * ⚠️ The target_tense column WAS MISSED here (fixed 2026-08-07).
     *
     * The target tense went through the whole chain -- TensePolicy.forSlot -> generateAndStore -> payload sent to Python
     * -> Python returns it -> PracticeQuestion holds it in the object -- and then was dropped at the very last write
     * because this INSERT does not list the column. No error, no warning: every generated question had
     * target_tense NULL, so the filter in findUnseenByTopicAndCriterionAndRankRange
     * (`question.target_tense IS NULL OR = :tense`) always fell into the first side and the whole enforcement
     * mechanism never ran.
     *
     * Adding a new column to practice_questions MUST also change this -- this is the only write path of
     * AI-generated questions, and it does not use an entity so Hibernate will not remind you.
     */
    @Modifying
    @Query(value = """
        INSERT INTO practice_questions (
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
