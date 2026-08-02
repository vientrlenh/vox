package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.sep.vox.application.query.dto.TranscriptInfo;
import com.sep.vox.infrastructure.persistence.entity.TopicSuggestionJpaEntity;

public interface SpringDataTopicSuggestionRepository
        extends JpaRepository<TopicSuggestionJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TopicSuggestionJpaEntity> findByIdAndStudentIdAndStatus(
        UUID id,
        UUID studentId,
        String status
    );

    int countByStudentIdAndStatus(UUID studentId, String status);

    List<TopicSuggestionJpaEntity> findByStudentIdAndStatusOrderByCreatedAtDesc(
        UUID studentId,
        String status
    );

    @Query(value = """
        SELECT COUNT(*)::int FROM topic_suggestion
        WHERE student_id = :studentId
          AND keyword IS NOT NULL
          AND created_at >= DATE_TRUNC('week', CURRENT_TIMESTAMP)
        """, nativeQuery = true)
    int countWeeklyKeywordRequests(@Param("studentId") UUID studentId);

    @Query(value = """
        SELECT DISTINCT profile.student_id
        FROM learner_profile profile
        WHERE EXISTS (
            SELECT 1
            FROM practice_session session
            WHERE session.student_id = profile.student_id
        )
          -- Cooldown chung áp cho CẢ hai nhánh dưới đây (kể cả nhánh "topic chưa xem < 8",
          -- vốn trước đây không có mốc thời gian nào cả -- một học sinh luyện tích cực, xử lý
          -- suggestion nhanh để giải phóng slot pending, có thể "due" lại ngay ở lượt quét kế
          -- tiếp, tức tối đa 24 lần gọi LLM/ngày cho đúng 1 học sinh). Tối đa 1 lần/ngày/học
          -- sinh, bất kể do lý do gì.
          AND NOT EXISTS (
              SELECT 1
              FROM topic_suggestion suggestion
              WHERE suggestion.student_id = profile.student_id
                AND suggestion.created_at >= CURRENT_TIMESTAMP - INTERVAL '1 day'
          )
          AND (
            NOT EXISTS (
                SELECT 1
                FROM topic_suggestion suggestion
                WHERE suggestion.student_id = profile.student_id
                  AND suggestion.created_at >=
                      CURRENT_TIMESTAMP - INTERVAL '30 days'
            )
            OR (
                SELECT COUNT(DISTINCT topic.id)
                FROM practice_topic topic
                JOIN practice_question question
                  ON question.practice_topic_id = topic.id
                 AND question.active = true
                WHERE topic.active = true
                  AND NOT EXISTS (
                      SELECT 1
                      FROM student_question_exposure exposure
                      WHERE exposure.student_id = profile.student_id
                        AND exposure.practice_question_id = question.id
                  )
            ) < 8
          )
          AND (
            SELECT COUNT(*)
            FROM topic_suggestion pending
            WHERE pending.student_id = profile.student_id
              AND pending.status = 'PENDING'
          ) < 2
        ORDER BY profile.student_id
        LIMIT :limit
        """, nativeQuery = true)
    List<UUID> findStudentsDueForSuggestionRefresh(@Param("limit") int limit);

    @Query(value = """
        SELECT DISTINCT session.id AS sessionId, turn.transcript AS transcript
        FROM practice_session session
        JOIN practice_item_response response
          ON response.practice_session_id = session.id
        JOIN practice_response_turn turn
          ON turn.practice_response_id = response.id
        WHERE session.student_id = :studentId
          AND session.started_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
          AND turn.transcript IS NOT NULL
        """, nativeQuery = true)
    List<TranscriptInfo> findRecentTranscripts(@Param("studentId") UUID studentId);
}
