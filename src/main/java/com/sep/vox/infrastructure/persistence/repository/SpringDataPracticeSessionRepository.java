package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.CriterionFrameworkInfo;
import com.sep.vox.application.query.dto.PracticeDashboardCountsInfo;
import com.sep.vox.application.query.dto.SessionRowInfo;
import com.sep.vox.infrastructure.persistence.entity.PracticeSessionJpaEntity;

public interface SpringDataPracticeSessionRepository
        extends JpaRepository<PracticeSessionJpaEntity, UUID> {

    Optional<PracticeSessionJpaEntity> findByIdAndStudentId(UUID id, UUID studentId);

    // Khoá row cho suốt transaction resolve-next-question (gói 11 mục 2.4 bước 4) -- một
    // request thứ 2 (Python retry sau timeout, xem practice_session_client.py) cho CÙNG
    // session phải đợi request đầu commit xong rồi mới đọc, chứ không được chạy song song
    // và cùng chọn/lưu 1 câu MAIN mới độc lập với nhau (race → có thể vỡ ràng buộc slot).
    @Query(value = "SELECT * FROM practice_session WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<PracticeSessionJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByIdAndStudentIdAndStatus(UUID id, UUID studentId, String status);

    int countByIdAndStudentIdAndStatus(UUID id, UUID studentId, String status);

    @Query(value = """
        SELECT session.id AS id,
               session.practice_paper_id AS practicePaperId,
               session.chosen_practice_topic_id AS chosenPracticeTopicId,
               topic.name AS topicName,
               session.origin AS origin,
               session.status AS status,
               session.abandon_diagnosis AS abandonDiagnosis,
               session.overall_score AS overallScore,
               session.graded_seconds AS gradedSeconds,
               session.offered_topic_ids_json AS offeredTopicIdsJson,
               session.started_at AS startedAt,
               session.ended_at AS endedAt
        FROM practice_session session
        JOIN practice_topic topic ON topic.id = session.chosen_practice_topic_id
        WHERE session.id = :sessionId AND session.student_id = :studentId
        """, nativeQuery = true)
    Optional<SessionRowInfo> findSessionRow(
        @Param("sessionId") UUID sessionId,
        @Param("studentId") UUID studentId
    );

    @Query(value = """
        SELECT session.id AS id,
               session.practice_paper_id AS practicePaperId,
               session.chosen_practice_topic_id AS chosenPracticeTopicId,
               topic.name AS topicName,
               session.origin AS origin,
               session.status AS status,
               session.abandon_diagnosis AS abandonDiagnosis,
               session.overall_score AS overallScore,
               session.graded_seconds AS gradedSeconds,
               session.offered_topic_ids_json AS offeredTopicIdsJson,
               session.started_at AS startedAt,
               session.ended_at AS endedAt
        FROM practice_session session
        JOIN practice_topic topic ON topic.id = session.chosen_practice_topic_id
        WHERE session.id = :sessionId
        """, nativeQuery = true)
    Optional<SessionRowInfo> findSessionRowById(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT session.id AS id,
               session.practice_paper_id AS practicePaperId,
               session.chosen_practice_topic_id AS chosenPracticeTopicId,
               topic.name AS topicName,
               session.origin AS origin,
               session.status AS status,
               session.abandon_diagnosis AS abandonDiagnosis,
               session.overall_score AS overallScore,
               session.graded_seconds AS gradedSeconds,
               session.offered_topic_ids_json AS offeredTopicIdsJson,
               session.started_at AS startedAt,
               session.ended_at AS endedAt
        FROM practice_session session
        JOIN practice_topic topic ON topic.id = session.chosen_practice_topic_id
        WHERE session.student_id = :studentId
        ORDER BY session.started_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<SessionRowInfo> findHistory(@Param("studentId") UUID studentId, @Param("limit") int limit);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM practice_session session
            JOIN school_class_users student_membership
              ON student_membership.user_id = session.student_id
             AND student_membership.is_active = true
            JOIN school_classes class
              ON class.id = student_membership.school_class_id
            JOIN school_users teacher
              ON teacher.school_id = class.school_id
             AND teacher.user_id = :teacherId
             AND teacher.end_date IS NULL
            WHERE session.id = :sessionId
        )
        """, nativeQuery = true)
    boolean canTeacherReadSession(@Param("teacherId") UUID teacherId, @Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT *
        FROM practice_session
        WHERE status = 'IN_PROGRESS'
          AND last_heartbeat_at < :staleBefore
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<PracticeSessionJpaEntity> findStaleInProgressForUpdate(@Param("staleBefore") OffsetDateTime staleBefore);

    @Query(value = """
        SELECT rubric.id AS rubricCriterionId,
               rubric.code AS rubricCode,
               rubric.weight AS weight, rubric.min_score AS minScore, rubric.max_score AS maxScore,
               criterion.code AS frameworkCode,
               criterion.name AS frameworkName,
               criterion.description AS frameworkDescription,
               target.id AS targetBandId,
               target.code AS targetBandCode,
               target.label AS targetBandLabel,
               result.code AS bandCode,
               result.label AS bandLabel,
               result.result_band_order AS bandOrder,
               band.descriptor AS descriptor
        FROM practice_session session
        JOIN rubric_criterions rubric
          ON rubric.rubric_version_id = session.rubric_version_id
        JOIN framework_criteria criterion
          ON criterion.id = rubric.framework_criterion_id
        JOIN framework_result_bands target
          ON target.id = session.target_framework_band_id
        JOIN framework_criterion_bands band
          ON band.framework_criterion_id = criterion.id
        JOIN framework_result_bands result
          ON result.id = band.framework_result_band_id
        WHERE session.id = :sessionId
        ORDER BY rubric.criterion_order, result.result_band_order
        """, nativeQuery = true)
    List<CriterionFrameworkInfo> findCriteriaFrameworks(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT abandon_diagnosis
        FROM practice_session
        WHERE student_id = :studentId
          AND chosen_practice_topic_id = :topicId
          AND status = 'ABANDONED'
        ORDER BY started_at DESC
        LIMIT 1
        """, nativeQuery = true)
    List<String> findLastAbandonDiagnosis(
        @Param("studentId") UUID studentId,
        @Param("topicId") UUID topicId
    );

    @Query(value = """
        SELECT COUNT(*) AS sessionsDone,
               COALESCE(AVG(overall_score), 0) AS averageScore
        FROM practice_session
        WHERE student_id = :studentId AND status = 'COMPLETED'
        """, nativeQuery = true)
    PracticeDashboardCountsInfo findDashboardCounts(@Param("studentId") UUID studentId);

    @Query(value = """
        SELECT DISTINCT started_at::date AS d
        FROM practice_session
        WHERE student_id = :studentId AND status = 'COMPLETED'
        ORDER BY d DESC
        """, nativeQuery = true)
    List<java.time.LocalDate> findCompletedSessionDatesDesc(@Param("studentId") UUID studentId);
}
