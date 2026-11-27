package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.CriterionFrameworkInfo;
import com.sep.vox.application.query.dto.PracticeDashboardCountsInfo;
import com.sep.vox.application.query.dto.SessionRowInfo;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeSessionJpaEntity;

public interface SpringDataPracticeSessionRepository
        extends JpaRepository<PracticeSessionJpaEntity, UUID> {

    // Row lock for the whole resolve-next-question transaction (plan 11 section 2.4 step 4) -- a second
    // request (Python retry after a timeout, see practice_session_client.py) for the SAME
    // session must wait for the first request to commit before reading, and must not run in parallel
    // and each independently choose/save a new MAIN question (race → can break the slot constraint).
    @Query(value = "SELECT * FROM practice_sessions WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<PracticeSessionJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByIdAndStudentIdAndStatus(UUID id, UUID studentId, String status);

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
               session.ended_at AS endedAt,
               (
                   SELECT COUNT(*)
                   FROM practice_item_responses response
                   WHERE response.practice_session_id = session.id
                     AND NOT EXISTS (
                         SELECT 1
                         FROM practice_item_evaluations evaluation
                         WHERE evaluation.practice_response_id = response.id
                     )
               )::int AS pendingEvaluations,
               -- The practice grading scale is a FIXED 0-100 -- Azure returns HundredMark so this
               -- is the original scale. It is still returned to the client instead of letting the client know it: a denominator
               -- hardcoded in the UI is a bug we have already hit (printing " / 100" when the data was 0-10).
               0   AS scoreScaleMin,
               100 AS scoreScaleMax
        FROM practice_sessions session
        JOIN practice_topics topic ON topic.id = session.chosen_practice_topic_id
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
               session.ended_at AS endedAt,
               (
                   SELECT COUNT(*)
                   FROM practice_item_responses response
                   WHERE response.practice_session_id = session.id
                     AND NOT EXISTS (
                         SELECT 1
                         FROM practice_item_evaluations evaluation
                         WHERE evaluation.practice_response_id = response.id
                     )
               )::int AS pendingEvaluations,
               -- The practice grading scale is a FIXED 0-100 -- Azure returns HundredMark so this
               -- is the original scale. It is still returned to the client instead of letting the client know it: a denominator
               -- hardcoded in the UI is a bug we have already hit (printing " / 100" when the data was 0-10).
               0   AS scoreScaleMin,
               100 AS scoreScaleMax
        FROM practice_sessions session
        JOIN practice_topics topic ON topic.id = session.chosen_practice_topic_id
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
               session.ended_at AS endedAt,
               -- The same count the DETAIL screen uses to decide whether to show the "grading" banner
               -- (countAwaitingEvaluation). The list must ask the same question, otherwise
               -- two screens say two different things about the same session: the outside shows 72, tapping
               -- in says grading, and a moment later it becomes 76.
               --
               -- Computed inside the list query itself instead of one extra round per
               -- session: the history returns at most a few dozen rows, but the N+1 here is a real N+1.
               (
                   SELECT COUNT(*)
                   FROM practice_item_responses response
                   WHERE response.practice_session_id = session.id
                     AND NOT EXISTS (
                         SELECT 1
                         FROM practice_item_evaluations evaluation
                         WHERE evaluation.practice_response_id = response.id
                     )
               )::int AS pendingEvaluations
        FROM practice_sessions session
        JOIN practice_topics topic ON topic.id = session.chosen_practice_topic_id
        WHERE session.student_id = :studentId
        ORDER BY session.started_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<SessionRowInfo> findHistory(@Param("studentId") UUID studentId, @Param("limit") int limit);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM practice_sessions session
            JOIN school_class_users student_membership
              ON student_membership.user_id = session.student_id
             AND student_membership.is_active = true
            JOIN school_classes class
              ON class.id = student_membership.school_class_id
            JOIN school_users teacher
              ON teacher.school_id = class.school_id
             AND teacher.user_id = :teacherId
             AND (teacher.end_date IS NULL OR teacher.end_date >= CURRENT_TIMESTAMP)
            WHERE session.id = :sessionId
        )
        """, nativeQuery = true)
    boolean canTeacherReadSession(@Param("teacherId") UUID teacherId, @Param("sessionId") UUID sessionId);

    @Modifying
    @Query(value = """
        UPDATE practice_sessions
        SET last_heartbeat_at = CURRENT_TIMESTAMP
        WHERE id = :sessionId AND status = 'IN_PROGRESS'
        """, nativeQuery = true)
    void touchHeartbeat(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT *
        FROM practice_sessions
        WHERE status = 'IN_PROGRESS'
          AND last_heartbeat_at < :staleBefore
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<PracticeSessionJpaEntity> findStaleInProgressForUpdate(@Param("staleBefore") Instant staleBefore);

  
    // Does NOT filter question_complete any more (2026-08-07).
    //
    // Old rule: an abandoned question was still graded to collect weakness observations, but was not counted in the
    // session score -- "grading an unfinished question by the rubric of a complete one is certain to be low, penalizing the student
    // for losing connection is wrong".
    //
    // Two reasons for dropping it:
    //   1. The weakness profile was removed on 2026-08-06, so the "still grade to collect observations" side has no
    //      destination any more. Now it is graded and then thrown away.
    //   2. The "certain to be low" assumption is wrong. Measured on a real session on 2026-08-07: an abandoned
    //      question (2 turns, 26 seconds) scored 80.07 -- HIGHER than both completed questions (72.56 and 73.97).
    //
    // And when the abandoned question is the ONLY one, excluding it protects nothing: AVG over an empty set gives
    // NULL and the whole session shows 0 points. Protecting against being pulled down turns into wiping everything out.
    //
    // marked_invalid is the flag with the right role for "is it worth counting" -- ValidityNode has already raised that flag
    // for answers that are too short or off topic.
    @Modifying
    @Query(value = """
        UPDATE practice_sessions
        SET overall_score = (
            SELECT AVG(evaluation.item_score)
            FROM practice_item_responses response
            JOIN practice_item_evaluations evaluation
              ON evaluation.practice_response_id = response.id
            WHERE response.practice_session_id = :sessionId
              AND evaluation.marked_invalid = false
        )
        WHERE id = :sessionId
        """, nativeQuery = true)
    void refreshOverallScore(@Param("sessionId") UUID sessionId);

    /**
     * The grading framework sent down to Python for ONE practice session.
     *
     * <p>Since V13 it no longer goes through a rubric. Three differences from the old version:
     * <ul>
     *   <li>criteria come straight from {@code framework_criteria} instead of {@code rubric_criterions};</li>
     *   <li>a fixed 0-100 grading scale -- Azure returns HundredMark so this is the original scale, not
     *       an arbitrary choice, and there is no rubric to look up a range from;</li>
     *   <li>returns EXACTLY ONE band -- the band the student chose -- instead of the whole scale. Practice grades
     *       "how well it meets the description of this band", it does not classify, so the other bands have nothing
     *       to do in the prompt.</li>
     * </ul>
     *
     * <p>{@code framework_version_id} is derived from the target band itself rather than looking up the active framework
     * again: the band the student chose belongs to one specific version, and using it is the most consistent
     * even if an admin changes the framework midway.
     */
    @Query(value = """
        SELECT fc.id                     AS criterionId,
               fc.code                   AS criterionCode,
               0                         AS minScore,
               100                       AS maxScore,
               fc.code                   AS frameworkCode,
               fc.name                   AS frameworkName,
               fc.description            AS frameworkDescription,
               CAST(target.id AS varchar) AS targetBandId,
               target.code               AS targetBandCode,
               target.label              AS targetBandLabel,
               target.code               AS bandCode,
               target.label              AS bandLabel,
               target.result_band_order  AS bandOrder,
               fcb.descriptor            AS descriptor
        FROM practice_sessions session
        JOIN framework_result_bands target
          ON target.id = session.target_framework_band_id
        JOIN framework_criteria fc
          ON fc.framework_version_id = target.framework_version_id
        JOIN framework_criterion_bands fcb
          ON fcb.framework_criterion_id = fc.id
         AND fcb.framework_result_band_id = target.id
        WHERE session.id = :sessionId
        ORDER BY fc.code
        """, nativeQuery = true)
    List<CriterionFrameworkInfo> findCriteriaFrameworks(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT COUNT(*) AS sessionsDone,
               COALESCE(AVG(overall_score), 0) AS averageScore
        FROM practice_sessions
        WHERE student_id = :studentId AND status = 'COMPLETED'
        """, nativeQuery = true)
    PracticeDashboardCountsInfo findDashboardCounts(@Param("studentId") UUID studentId);

    @Query(value = """
        SELECT DISTINCT started_at::date AS d
        FROM practice_sessions
        WHERE student_id = :studentId AND status = 'COMPLETED'
        ORDER BY d DESC
        """, nativeQuery = true)
    List<java.time.LocalDate> findCompletedSessionDatesDesc(@Param("studentId") UUID studentId);
}
